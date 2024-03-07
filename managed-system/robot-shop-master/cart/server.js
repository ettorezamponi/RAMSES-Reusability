const instana = require('@instana/collector');
// init tracing
// MUST be done before loading anything else!
instana({
    tracing: {
        enabled: true
    }
});

const redis = require('ioredis');
const request = require('request');
const bodyParser = require('body-parser');
const express = require('express');
const pino = require('pino');
const expPino = require('express-pino-logger');
// Prometheus
const promClient = require('prom-client');
const Registry = promClient.Registry;
const register = new Registry();
// Modules to collect metrics
const pidusage = require('pidusage');
const diskusage = require('diskusage');
const responseTime = require('response-time');

const counter = new promClient.Counter({
    name: 'items_added',
    help: 'running count of items added to cart',
    registers: [register]
});
//TODO a volte il valore della cpu è zero!!
const cpuUsageMetric = new promClient.Gauge({
    name: 'system_cpu_usage',
    help: 'System CPU usage from this service',
    registers: [register]
});
const diskTotalSpace = new promClient.Gauge({
    name: 'disk_total_bytes',
    help: 'Total disk space',
    registers: [register]
});
const diskFreeSpace = new promClient.Gauge({
    name: 'disk_free_bytes',
    help: 'Total free disk space',
    registers: [register]
});
const httpMaxRequest = new promClient.Gauge({
    name: 'http_server_requests_seconds_max',
    help: 'Max HTTP duration request',
    labelNames: ['exception','method','outcome','status','uri'],
    registers: [register]
});
const httpServerRequest = new promClient.Histogram({
    name: 'http_server_requests_seconds',
    help: 'Max HTTP duration request',
    labelNames: ['exception','method','outcome','status','uri'],
    registers: [register]
});

const app = express();

// Error injection
let art_delay = 0;
let delay_art_init = 4 * 1000 * 60
let delay_art_finish = 7 * 1000 * 60

let outcome = 'SUCCESS'
let status = '200'
let delay_availability_init = 0 * 1000 * 60
let delay_availability_finish = 0 * 1000 * 60

async function updateMetrics() {
    // CPU metrics
    try {
        const stats = await pidusage(process.pid);
        const cpuUsagePercent = stats.cpu;
        cpuUsageMetric.set(cpuUsagePercent);
    } catch (error) {
        console.error('Errore durante la lettura della CPU Usage:', error);
    }
    // Disk space metrics
    try {
        const diskInfo = diskusage.checkSync('/');
        const totalDiskSpace = diskInfo.total;
        const freeDiskSpace = diskInfo.available;

        diskTotalSpace.set(totalDiskSpace);
        diskFreeSpace.set(freeDiskSpace);
    } catch (error) {
        console.error('Errore durante la lettura dello spazio su disco:', error);
    }
};

// Usa il middleware response-time per ottenere la durata delle richieste
app.use(responseTime());
let maxDurationSuccess = 0;
let maxDurationError = 0;
app.use((req, res, next) => {
    const start = new Date();

    res.on('finish', () => {
        const end = new Date();
        const durationInSeconds = (end - start) / 1000;

        httpServerRequest.labels('None', req.method, outcome, status, req.originalUrl).observe(durationInSeconds);

        if (durationInSeconds > maxDurationSuccess ) {
            maxDurationSuccess = durationInSeconds;
            httpMaxRequest.labels('None', req.method,outcome,status, req.originalUrl).set(durationInSeconds);
        }

    });

    next();
});


const Eureka = require('eureka-js-client').Eureka;
const client = new Eureka({
    // application instance information
    instance: {
        app: 'rs-cart',
        instanceId: 'cart@rs-cart:8080',
        hostName: 'localhost',
        ipAddr: '127.0.0.1',
        port:  {
            '$': 8001,
            '@enabled': 'true',
        },
        vipAddress: 'cart',
        statusPageUrl: 'http://localhost:8080/api/cart/metrics',
        //TODO aggiusta metriche prese dal probe
        healthCheckUrl: 'http://cart:8080/metrics',
        dataCenterInfo:  {
            '@class': 'com.netflix.appinfo.InstanceInfo$DefaultDataCenterInfo',
            name: 'MyOwn',
        }
    },
    eureka: {
        // eureka server host / port
        host: 'sefa-eureka',
        port: 58082,
        servicePath: '/eureka/apps/'
    },
});

client.logger.level('debug');

setTimeout(async function () {
    try {
        await client.start(function (error) {
            console.log(error || 'complete');
        });
    } catch (error) {
        console.error("ERROR DURING EUREKA REGISTRATION")
    }
}, 10000);

var redisConnected = false;

var redisHost = process.env.REDIS_HOST || 'redis'
var catalogueHost = process.env.CATALOGUE_HOST || 'catalogue'

const logger = pino({
    level: 'info',
    prettyPrint: false,
    useLevelLabels: true
});
const expLogger = expPino({
    logger: logger
});

app.use(expLogger);

app.use((req, res, next) => {
    res.set('Timing-Allow-Origin', '*');
    res.set('Access-Control-Allow-Origin', '*');
    next();
});

app.use((req, res, next) => {
    let dcs = [
        "asia-northeast2",
        "asia-south1",
        "europe-west3",
        "us-east1",
        "us-west1"
    ];
    let span = instana.currentSpan();
    span.annotate('custom.sdk.tags.datacenter', dcs[Math.floor(Math.random() * dcs.length)]);

    next();
});

app.use(bodyParser.urlencoded({ extended: true }));
app.use(bodyParser.json());

app.get('/health', (req, res) => {
    var stat = {
        app: 'OK',
        redis: redisConnected
    };
    res.json(stat);
});

// Prometheus
app.get('/metrics', async (req, res) => {
    updateMetrics();
    res.header('Content-Type', 'text/plain');
    res.send(await register.metrics());
});


// get cart with id
app.get('/cart/:id', (req, res) => {
    setTimeout(() => {

        redisClient.get(req.params.id, (err, data) => {
            if (err) {
                req.log.error('ERROR', err);
                res.status(500).send(err);
            } else {
                if (data == null) {
                    res.status(404).send('cart not found');
                } else {
                    res.set('Content-Type', 'application/json');
                    res.send(data);
                }
            }
        });
    }, art_delay);
});

// delete cart with id
app.delete('/cart/:id', (req, res) => {
    setTimeout(() => {

        redisClient.del(req.params.id, (err, data) => {
            if(err) {
                req.log.error('ERROR', err);
                res.status(500).send(err);
            } else {
                if(data == 1) {
                    res.send('OK');
                } else {
                    res.status(404).send('cart not found');
                }
            }
        });
    }, art_delay);
});

// rename cart i.e. at login
app.get('/rename/:from/:to', (req, res) => {
    setTimeout(() => {

        redisClient.get(req.params.from, (err, data) => {
            if(err) {
                req.log.error('ERROR', err);
                res.status(500).send(err);
            } else {
                if(data == null) {
                    res.status(404).send('cart not found');
                } else {
                    var cart = JSON.parse(data);
                    saveCart(req.params.to, cart).then((data) => {
                        res.json(cart);
                    }).catch((err) => {
                        req.log.error(err);
                        res.status(500).send(err);
                    });
                }
            }
        });
    }, art_delay);
});

// update/create cart
app.get('/add/:id/:sku/:qty', (req, res) => {
    setTimeout(() => {

        // check quantity
        var qty = parseInt(req.params.qty);
        if(isNaN(qty)) {
            req.log.warn('quantity not a number');
            res.status(400).send('quantity must be a number');
            return;
        } else if(qty < 1) {
            req.log.warn('quantity less than one');
            res.status(400).send('quantity has to be greater than zero');
            return;
        }

        // look up product details
        getProduct(req.params.sku).then((product) => {
            req.log.info('got product', product);
            if(!product) {
                res.status(404).send('product not found');
                return;
            }
            // is the product in stock?
            if(product.instock == 0) {
                res.status(404).send('out of stock');
                return;
            }
            // does the cart already exist?
            redisClient.get(req.params.id, (err, data) => {
                if(err) {
                    req.log.error('ERROR', err);
                    res.status(500).send(err);
                } else {
                    var cart;
                    if(data == null) {
                        // create new cart
                        cart = {
                            total: 0,
                            tax: 0,
                            items: []
                        };
                    } else {
                        cart = JSON.parse(data);
                    }
                    req.log.info('got cart', cart);
                    // add sku to cart
                    var item = {
                        qty: qty,
                        sku: req.params.sku,
                        name: product.name,
                        price: product.price,
                        subtotal: qty * product.price
                    };
                    var list = mergeList(cart.items, item, qty);
                    cart.items = list;
                    cart.total = calcTotal(cart.items);
                    // work out tax
                    cart.tax = calcTax(cart.total);

                    // save the new cart
                    saveCart(req.params.id, cart).then((data) => {
                        counter.inc(qty);
                        res.json(cart);
                    }).catch((err) => {
                        req.log.error(err);
                        res.status(500).send(err);
                    });
                }
            });
        }).catch((err) => {
            req.log.error(err);
            res.status(500).send(err);
        });
    }, art_delay);
});

// update quantity - remove item when qty == 0
app.get('/update/:id/:sku/:qty', (req, res) => {
    setTimeout(() => {

        // check quantity
        var qty = parseInt(req.params.qty);
        if(isNaN(qty)) {
            req.log.warn('quanity not a number');
            res.status(400).send('quantity must be a number');
            return;
        } else if(qty < 0) {
            req.log.warn('quantity less than zero');
            res.status(400).send('negative quantity not allowed');
            return;
        }

        // get the cart
        redisClient.get(req.params.id, (err, data) => {
            if(err) {
                req.log.error('ERROR', err);
                res.status(500).send(err);
            } else {
                if(data == null) {
                    res.status(404).send('cart not found');
                } else {
                    var cart = JSON.parse(data);
                    var idx;
                    var len = cart.items.length;
                    for(idx = 0; idx < len; idx++) {
                        if(cart.items[idx].sku == req.params.sku) {
                            break;
                        }
                    }
                    if(idx == len) {
                        // not in list
                        res.status(404).send('not in cart');
                    } else {
                        if(qty == 0) {
                            cart.items.splice(idx, 1);
                        } else {
                            cart.items[idx].qty = qty;
                            cart.items[idx].subtotal = cart.items[idx].price * qty;
                        }
                        cart.total = calcTotal(cart.items);
                        // work out tax
                        cart.tax = calcTax(cart.total);
                        saveCart(req.params.id, cart).then((data) => {
                            res.json(cart);
                        }).catch((err) => {
                            req.log.error(err);
                            res.status(500).send(err);
                        });
                    }
                }
            }
        });
    }, art_delay);
});

// add shipping
app.post('/shipping/:id', (req, res) => {
    setTimeout(() => {

        var shipping = req.body;
        if(shipping.distance === undefined || shipping.cost === undefined || shipping.location == undefined) {
            req.log.warn('shipping data missing', shipping);
            res.status(400).send('shipping data missing');
        } else {
            // get the cart
            redisClient.get(req.params.id, (err, data) => {
                if(err) {
                    req.log.error('ERROR', err);
                    res.status(500).send(err);
                } else {
                    if(data == null) {
                        req.log.info('no cart for', req.params.id);
                        res.status(404).send('cart not found');
                    } else {
                        var cart = JSON.parse(data);
                        var item = {
                            qty: 1,
                            sku: 'SHIP',
                            name: 'shipping to ' + shipping.location,
                            price: shipping.cost,
                            subtotal: shipping.cost
                        };
                        // check shipping already in the cart
                        var idx;
                        var len = cart.items.length;
                        for(idx = 0; idx < len; idx++) {
                            if(cart.items[idx].sku == item.sku) {
                                break;
                            }
                        }
                        if(idx == len) {
                            // not already in cart
                            cart.items.push(item);
                        } else {
                            cart.items[idx] = item;
                        }
                        cart.total = calcTotal(cart.items);
                        // work out tax
                        cart.tax = calcTax(cart.total);

                        // save the updated cart
                        saveCart(req.params.id, cart).then((data) => {
                            res.json(cart);
                        }).catch((err) => {
                            req.log.error(err);
                            res.status(500).send(err);
                        });
                    }
                }
            });
        }
    }, art_delay);
});

function mergeList(list, product, qty) {
    var inlist = false;
    // loop through looking for sku
    var idx;
    var len = list.length;
    for(idx = 0; idx < len; idx++) {
        if(list[idx].sku == product.sku) {
            inlist = true;
            break;
        }
    }

    if(inlist) {
        list[idx].qty += qty;
        list[idx].subtotal = list[idx].price * list[idx].qty;
    } else {
        list.push(product);
    }

    return list;
}

function calcTotal(list) {
    var total = 0;
    for(var idx = 0, len = list.length; idx < len; idx++) {
        total += list[idx].subtotal;
    }

    return total;
}

function calcTax(total) {
    // tax @ 20%
    return (total - (total / 1.2));
}

function getProduct(sku) {
    return new Promise((resolve, reject) => {
        request('http://' + catalogueHost + ':8080/product/' + sku, (err, res, body) => {
            if(err) {
                reject(err);
            } else if(res.statusCode != 200) {
                resolve(null);
            } else {
                // return object - body is a string
                // TODO - catch parse error
                resolve(JSON.parse(body));
            }
        });
    });
}

function saveCart(id, cart) {
    logger.info('saving cart', cart);
    return new Promise((resolve, reject) => {
        redisClient.setex(id, 3600, JSON.stringify(cart), (err, data) => {
            if(err) {
                reject(err);
            } else {
                resolve(data);
            }
        });
    });
}

function enable_delay_art_after(milliseconds) {
    setTimeout(() => {
        art_delay = 500;
        console.log("Delay ART modified to " + art_delay)
    }, milliseconds);
}

function disable_delay_art_after(milliseconds) {
    setTimeout(() => {
        art_delay = 0;
        console.log("Delay ART back to normal values")
    }, milliseconds);
}

function enable_slow_availability_after(millisec) {
    setTimeout(() => {
        outcome = 'SERVER_ERROR'
        status = '500'
        console.log('Server_error injected')
    }, millisec);
}

function disable_slow_availability_after(millisec) {
    setTimeout(() => {
        outcome = 'SUCCESS'
        status = '200'
        console.log('Server requests back to normal')
    }, millisec);
}

// connect to Redis
var redisClient = redis.createClient({
    host: redisHost
});

redisClient.on('error', (e) => {
    logger.error('Redis ERROR', e);
});
redisClient.on('ready', (r) => {
    logger.info('Redis READY', r);
    redisConnected = true;
});

// fire it up!
const port = process.env.CART_SERVER_PORT || '8080';
app.listen(port, () => {
    logger.info('Started on port', port);
});

// ERROR INJECTION
enable_delay_art_after(delay_art_init);
disable_delay_art_after(delay_art_finish)
enable_slow_availability_after(delay_availability_init)
disable_slow_availability_after(delay_availability_finish)
