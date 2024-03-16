FROM python:3

RUN pip install kafka-python-ng requests

COPY src /app/

WORKDIR /app

CMD ["python", "kafka_producer.py"]