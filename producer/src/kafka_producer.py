import time
import json
import requests
from kafka import KafkaProducer
import os

kfk_bootstrap_server = 'kafka:29092'

def openweather_key():
    return os.environ.get('OPENWEATHER_API_KEY')

def kafka_producer() -> KafkaProducer:
    return KafkaProducer(
        bootstrap_servers=[kfk_bootstrap_server],
        value_serializer=lambda x: json.dumps(x).encode('utf-8')
    )

def get_weather_infos(openweather_endpoint:str) -> dict:
    api_response = requests.get(openweather_endpoint)
    json_data = api_response.json()
    city_id = json_data['id']
    city_name = json_data['name']
    lat = json_data['coord']['lat']
    lon = json_data['coord']['lon']
    country = json_data['sys']['country']
    temp = json_data['main']['temp']
    max_temp = json_data['main']['temp_max']
    min_temp = json_data['main']['temp_min']
    feels_like = json_data['main']['feels_like']
    humidity = json_data['main']['humidity']

    json_msg = {
        'created_at': time.strftime('%Y-%m-%d %H:%M:%S'),
        'city_id': city_id,
        'city_name': city_name,
        'lat': lat,
        'lon': lon,
        'country': country,
        'temp': temp,
        'max_temp': max_temp,
        'min_temp': min_temp,
        'feels_like': feels_like,
        'humidity': humidity
    }
    return json_msg

def main():
    kfk_topic = 'openweather'
    api_key = openweather_key()
    cities = ('Mumbai', 'Hyderabad', 'Dhaka')
    while True:
        print(f'Running at {time.strftime("%Y-%m-%d %H:%M:%S")} ...')
        for city in cities:
            openweather_endpoint = f'http://api.openweathermap.org/data/2.5/weather?q={city}&appid={api_key}&units=metric'
            json_msg = get_weather_infos(openweather_endpoint)
            producer = kafka_producer()
            if isinstance(producer, KafkaProducer):
                producer.send(kfk_topic, json_msg).get(timeout=30)
                print(f'Published {city}: {json.dumps(json_msg)}')
                sleep = (24 * 60 * 60) / 800
                print(f'Waiting {sleep} seconds ...')
                time.sleep(sleep)

if __name__=="__main__":
    main()