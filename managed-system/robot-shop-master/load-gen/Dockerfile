FROM python:3.9

# Some default values
ENV HOST="http://localhost:8080/" \
    SILENT=0 \
    NUM_CLIENTS=15 \
    ERROR=0 \
    RUN_TIME=15m
    

WORKDIR /load

COPY requirements.txt /load/

RUN pip install -r requirements.txt

COPY entrypoint.sh /load/
COPY robot-shop.py /load/

CMD ["./entrypoint.sh"]

