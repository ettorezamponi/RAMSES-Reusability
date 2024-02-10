# export PATH="/Users/ettorezamponi/Library/Python/3.9/bin"

import random

import instana
import os
import sys
import time
import logging
import uuid
import json
import requests
import traceback
from flask import Flask
from flask import Response
from flask import request
from flask import jsonify
from rabbitmq import Publisher
# Metrics
import psutil

# Prometheus
import prometheus_client
from prometheus_client import Counter
from prometheus_client import Gauge
from prometheus_client import Histogram

import py_eureka_client.eureka_client as eureka_client
# The flowing code will register your server to eureka server and also start to send heartbeat every 30 seconds
# https://github.com/keijack/python-eureka-client/blob/main/py_eureka_client/eureka_client.py
eureka_client.init(eureka_server="http://sefa-eureka:58082/eureka/",
                   app_name="rs-payment",
                   instance_id="payment@rs-payment:8080",
                   instance_port=8002,
                   instance_host="localhost",
                   health_check_url="http://payment:8080/metrics",
                   vip_adr="payment")

app = Flask(__name__)
app.logger.setLevel(logging.INFO)

CART = os.getenv('CART_HOST', 'cart')
USER = os.getenv('USER_HOST', 'user')
PAYMENT_GATEWAY = os.getenv('PAYMENT_GATEWAY', 'https://paypal.com/')

# Prometheus
PromMetrics = {}
PromMetrics['SOLD_COUNTER'] = Counter('sold_count', 'Running count of items sold')
PromMetrics['AUS'] = Histogram('units_sold', 'Avergae Unit Sale', buckets=(1, 2, 5, 10, 100))
PromMetrics['AVS'] = Histogram('cart_value', 'Avergae Value Sale', buckets=(100, 200, 500, 1000, 2000, 5000, 10000))
# Added for RAMSES
PromMetrics['SYSTEM_CPU_USAGE'] = Gauge('system_cpu_usage', 'System CPU Usage')
PromMetrics['DISK_TOTAL_BYTES'] = Gauge('disk_total_bytes', 'Total Disk Bytes')
PromMetrics['DISK_FREE_BYTES'] = Gauge('disk_free_bytes', 'Free Disk Bytes')
PromMetrics['HTTP_SERVER_REQUESTS_SECONDS_MAX'] = Gauge(
    'http_server_requests_seconds_max',
    'HTTP Server Requests Duration (seconds, max)',
    labelnames=['exception', 'method', 'outcome', 'status', 'uri'],
)
PromMetrics['HTTP_SERVER_REQUESTS_SECONDS'] = Histogram(
    'http_server_requests_seconds',
    'HTTP Server Requests Duration (seconds)',
    labelnames=['exception', 'method', 'outcome', 'status', 'uri'],
    buckets=(1, 2, 5, 10, 30, 60, float('inf'))
)

max_value1 = float('-inf')
max_value2 = float('-inf')

@app.errorhandler(Exception)
def exception_handler(err):
    app.logger.error(str(err))
    return str(err), 500


@app.route('/health', methods=['GET'])
def health():
    global max_value1
    try:
        start_time = time.time()
        print("AZIONE CALCOLATA, ")
        duration_seconds = time.time() - start_time

        exception = 'None'
        method = request.method
        uri = request.path

        PromMetrics['HTTP_SERVER_REQUESTS_SECONDS'].labels(
            exception=exception, method=method, outcome='SUCCESS', status='200', uri=uri
        ).observe(duration_seconds)

        if duration_seconds > max_value1:
            PromMetrics['HTTP_SERVER_REQUESTS_SECONDS_MAX'].labels(
                exception=exception,
                method=method,
                outcome='SUCCESS',
                status='200',
                uri=uri,
            ).set(duration_seconds)
            max_value1 = duration_seconds

        return f'Ok! Max value: {max_value1}, Last response {duration_seconds}'

    except Exception as e:
        start_time = time.time()
        print("AZIONE CALCOLATA, ")
        duration_error_seconds = time.time() - start_time

        method = request.method
        uri = request.path
        PromMetrics['HTTP_SERVER_REQUESTS_SECONDS'].labels(
            exception='None', method=method, outcome='SERVER_ERROR', status='500', uri=uri
        ).observe(duration_error_seconds)

        return 'Internal Server Error:', e


# Prometheus
@app.route('/metrics', methods=['GET'])
def metrics():
    cpu_usage_percent = psutil.cpu_percent(interval=1)/100
    PromMetrics['SYSTEM_CPU_USAGE'].set(cpu_usage_percent)

    disk_usage = psutil.disk_usage('/')
    PromMetrics['DISK_TOTAL_BYTES'].set(disk_usage.total)
    PromMetrics['DISK_FREE_BYTES'].set(disk_usage.free)

    res = [prometheus_client.generate_latest(m) for m in PromMetrics.values()]

    return Response(res, mimetype='text/plain')


@app.route('/pay/<id>', methods=['POST'])
def pay(id):
    global max_value2
    start_time = time.time()
    app.logger.info('payment for {}'.format(id))
    cart = request.get_json()
    app.logger.info(cart)

    anonymous_user = True

    # check user exists
    try:
        req = requests.get('http://{user}:8080/check/{id}'.format(user=USER, id=id))
    except requests.exceptions.RequestException as err:
        app.logger.error(err)
        return str(err), 500
    if req.status_code == 200:
        anonymous_user = False

    # check that the cart is valid
    # this will blow up if the cart is not valid
    has_shipping = False
    for item in cart.get('items'):
        if item.get('sku') == 'SHIP':
            has_shipping = True

    if cart.get('total', 0) == 0 or has_shipping == False:
        app.logger.warn('cart not valid')
        return 'cart not valid', 400

    # dummy call to payment gateway, hope they dont object
    try:
        req = requests.get(PAYMENT_GATEWAY)
        app.logger.info('{} returned {}'.format(PAYMENT_GATEWAY, req.status_code))
    except requests.exceptions.RequestException as err:
        app.logger.error(err)
        return str(err), 500
    if req.status_code != 200:
        return 'payment error', req.status_code

    # Prometheus
    # items purchased
    item_count = countItems(cart.get('items', []))
    PromMetrics['SOLD_COUNTER'].inc(item_count)
    PromMetrics['AUS'].observe(item_count)
    PromMetrics['AVS'].observe(cart.get('total', 0))

    # Generate order id
    orderid = str(uuid.uuid4())
    queueOrder({ 'orderid': orderid, 'user': id, 'cart': cart })

    # add to order history
    if not anonymous_user:
        try:
            req = requests.post('http://{user}:8080/order/{id}'.format(user=USER, id=id),
                    data=json.dumps({'orderid': orderid, 'cart': cart}),
                    headers={'Content-Type': 'application/json'})
            app.logger.info('order history returned {}'.format(req.status_code))
        except requests.exceptions.RequestException as err:
            app.logger.error(err)
            return str(err), 500

    # delete cart
    try:
        req = requests.delete('http://{cart}:8080/cart/{id}'.format(cart=CART, id=id));
        app.logger.info('cart delete returned {}'.format(req.status_code))
    except requests.exceptions.RequestException as err:
        app.logger.error(err)
        return str(err), 500
    if req.status_code != 200:
        return 'order history update error', req.status_code

    duration_seconds = time.time() - start_time

    exception = 'None'
    method = request.method
    uri = request.path

    PromMetrics['HTTP_SERVER_REQUESTS_SECONDS'].labels(
        exception=exception, method=method, outcome='SUCCESS', status='200', uri=uri
    ).observe(duration_seconds)

    if duration_seconds > max_value2:
        PromMetrics['HTTP_SERVER_REQUESTS_SECONDS_MAX'].labels(
            exception=exception,
            method=method,
            outcome='SUCCESS',
            status='200',
            uri=uri,
        ).set(duration_seconds)
        max_value2 = duration_seconds

    return jsonify({ 'orderid': orderid })


def queueOrder(order):
    app.logger.info('queue order')

    # For screenshot demo requirements optionally add in a bit of delay
    delay = int(os.getenv('PAYMENT_DELAY_MS', 0))
    time.sleep(delay / 1000)

    headers = {}
    publisher.publish(order, headers)


def countItems(items):
    count = 0
    for item in items:
        if item.get('sku') != 'SHIP':
            count += item.get('qty')

    return count


# RabbitMQ
publisher = Publisher(app.logger)

if __name__ == "__main__":
    sh = logging.StreamHandler(sys.stdout)
    sh.setLevel(logging.INFO)
    fmt = logging.Formatter('%(asctime)s - %(name)s - %(levelname)s - %(message)s')
    app.logger.info('Payment gateway {}'.format(PAYMENT_GATEWAY))
    port = int(os.getenv("SHOP_PAYMENT_PORT", "8080"))
    app.logger.info('Starting on port {}'.format(port))
    app.run(host='0.0.0.0', port=port)
