import akka.Done
import akka.actor.typed.ActorSystem
import akka.kafka.{ProducerSettings}
import org.apache.kafka.common.serialization.StringSerializer
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import scala.concurrent.duration.DurationInt
import scala.concurrent.Future
import scala.util.{Failure, Success}
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import akka.http.scaladsl.unmarshalling.Unmarshal
import upickle.default.{ReadWriter => RW, macroRW, read => json}
import java.time.format.DateTimeFormatter

@main def hello(): Unit = {
  given system: ActorSystem[Nothing] =
    ActorSystem(OpenweatherProducer(), "OpenweatherProducer")
  Await.result(system.whenTerminated, Duration.Inf)
}

object OpenweatherProducer {
  val kfk_bootstrap_server = "localhost:9092"
  val cities = Seq("Mumbai", "Hyderabad", "Dhaka")
  val api_key = sys.env("OPENWEATHER_API_KEY")
  val openweather_endpoint = (city: String) =>
    s"http://api.openweathermap.org/data/2.5/weather?q=${city}&appid=${api_key}&units=metric"

  case object Tick
  def apply() = Behaviors.withTimers { timers =>
    timers.startTimerWithFixedDelay(Tick, 10.seconds)
    Behaviors.receive { (context, message) =>
      val config =
        context.system.settings.config.getConfig("akka.kafka.producer")
      val producerSettings =
        ProducerSettings(config, new StringSerializer, new StringSerializer)
          .withBootstrapServers(kfk_bootstrap_server)

      given system: ActorSystem[Nothing] = context.system
      given ec: scala.concurrent.ExecutionContext = context.executionContext

      // for each city, get the weather data
      for (city <- cities) {

        val responseFuture: Future[HttpResponse] =
          Http()
            .singleRequest(
              HttpRequest(uri = openweather_endpoint(cities.head))
            )

        // convert the response to a WeatherData
        responseFuture
          .flatMap { res =>
            {
              Unmarshal(res).to[String].map { data =>
                json[WeatherResponse](data)
              }
            }
          }
          .map(weatherResponse => {
            val created_at = java.time.LocalDateTime.now()
            WeatherData(
              created_at = DateTimeFormatter
                .ofPattern("yyyy-MM-dd HH:mm:ss")
                .format(created_at),
              city_id = weatherResponse.id,
              city_name = weatherResponse.name,
              lat = weatherResponse.coord.lat,
              lon = weatherResponse.coord.lon,
              country = weatherResponse.sys.country,
              temp = weatherResponse.main.temp,
              max_temp = weatherResponse.main.temp_max,
              min_temp = weatherResponse.main.temp_min,
              feels_like = weatherResponse.main.feels_like,
              humidity = weatherResponse.main.humidity
            )
          })
          .onComplete {
            case Success(value) =>
              println(value)
            // publish the WeatherData to Kafka topic with Alpakka Kafka

            case Failure(e) =>
              println(s"An error occurred: $e")
          }
      }

      Behaviors.same
    }
  }
}

// import time
// import json
// import requests
// from kafka import KafkaProducer
// from configparser import ConfigParser

// kfk_bootstrap_server = 'kafka:29092'

// def openweather_key(filename='openweather_key.ini', section='openweather'):
//     parser = ConfigParser()
//     parser.read(filename)
//     if parser.has_section(section):
//         params = parser.items(section)
//         return params[0][1]
//     else:
//         raise Exception(f'Section {section} not found in {filename}')

// def kafka_producer() -> KafkaProducer:
//     return KafkaProducer(
//         bootstrap_servers=[kfk_bootstrap_server],
//         value_serializer=lambda x: json.dumps(x).encode('utf-8')
//     )

// def get_weather_infos(openweather_endpoint:str) -> dict:
//     api_response = requests.get(openweather_endpoint)
//     json_data = api_response.json()
//     city_id = json_data['id']
//     city_name = json_data['name']
//     lat = json_data['coord']['lat']
//     lon = json_data['coord']['lon']
//     country = json_data['sys']['country']
//     temp = json_data['main']['temp']
//     max_temp = json_data['main']['temp_max']
//     min_temp = json_data['main']['temp_min']
//     feels_like = json_data['main']['feels_like']
//     humidity = json_data['main']['humidity']

//     json_msg = {
//         'created_at': time.strftime('%Y-%m-%d %H:%M:%S'),
//         'city_id': city_id,
//         'city_name': city_name,
//         'lat': lat,
//         'lon': lon,
//         'country': country,
//         'temp': temp,
//         'max_temp': max_temp,
//         'min_temp': min_temp,
//         'feels_like': feels_like,
//         'humidity': humidity
//     }
//     return json_msg

// def main():
//     kfk_topic = 'openweather'
//     api_key = openweather_key()
//     cities = ('Mumbai', 'Hyderabad', 'Dhaka')
//     while True:
//         print(f'Running at {time.strftime("%Y-%m-%d %H:%M:%S")} ...')
//         for city in cities:
//             openweather_endpoint = f'http://api.openweathermap.org/data/2.5/weather?q={city}&appid={api_key}&units=metric'
//             json_msg = get_weather_infos(openweather_endpoint)
//             producer = kafka_producer()
//             if isinstance(producer, KafkaProducer):
//                 producer.send(kfk_topic, json_msg).get(timeout=30)
//                 print(f'Published {city}: {json.dumps(json_msg)}')
//                 sleep = (24 * 60 * 60) / 800
//                 print(f'Waiting {sleep} seconds ...')
//                 time.sleep(sleep)

// if __name__=="__main__":
//     main()

case class WeatherData(
    created_at: String,
    city_id: Int,
    city_name: String,
    lat: Double,
    lon: Double,
    country: String,
    temp: Double,
    max_temp: Double,
    min_temp: Double,
    feels_like: Double,
    humidity: Int
)

case class Coord(
    lon: Float,
    lat: Float
)
object Coord {
  implicit val rw: RW[Coord] = macroRW
}

case class Weather(
    id: Int,
    main: String,
    description: String
)
object Weather {
  implicit val rw: RW[Weather] = macroRW
}

case class Main(
    temp: Float,
    pressure: Float,
    humidity: Int,
    temp_min: Float,
    temp_max: Float,
    feels_like: Float
)
object Main {
  implicit val rw: RW[Main] = macroRW
}

case class Wind(
    speed: Float,
    deg: Float
)
object Wind {
  implicit val rw: RW[Wind] = macroRW
}

case class Clouds(
    all: Int
)
object Clouds {
  implicit val rw: RW[Clouds] = macroRW
}

case class Sys(
    country: String,
    sunrise: Int,
    sunset: Int
)
object Sys {
  implicit val rw: RW[Sys] = macroRW
}

case class WeatherResponse(
    id: Int,
    coord: Coord,
    weather: List[Weather],
    main: Main,
    // visibility: Option[Int],
    wind: Wind,
    clouds: Clouds,
    sys: Sys,
    name: String
)
object WeatherResponse {
  implicit val rw: RW[WeatherResponse] = macroRW
}

case class OpenWeatherBaseCity(
    id: Int,
    name: String,
    lon: Float,
    lat: Float
)
object OpenWeatherBaseCity {
  implicit val rw: RW[OpenWeatherBaseCity] = macroRW
}

// {"coord":{"lon":72.8479,"lat":19.0144},"weather":[{"id":711,"main":"Smoke","description":"smoke","icon":"50d"}],"base":"stations","main":{"temp":30.99,"feels_like":31.2,"temp_min":27.94,"temp_max":30.99,"pressure":1013,"humidity":42},"visibility":3000,"wind":{"speed":5.66,"deg":280},"clouds":{"all":20},"dt":1710573150,"sys":{"type":1,"id":9052,"country":"IN","sunrise":1710551761,"sunset":1710595109},"timezone":19800,"id":1275339,"name":"Mumbai","cod":200}
// {"coord":{"lon":72.8479,"lat":19.0144},"weather":[{"id":711,"main":"Smoke","description":"smoke","icon":"50d"}],"base":"stations","main":{"temp":30.99,"feels_like":31.2,"temp_min":27.94,"temp_max":30.99,"pressure":1013,"humidity":42},"visibility":3000,"wind":{"speed":5.66,"deg":280},"clouds":{"all":20},"dt":1710573150,"sys":{"type":1,"id":9052,"country":"IN","sunrise":1710551761,"sunset":1710595109},"timezone":19800,"id":1275339,"name":"Mumbai","cod":200}
// {"coord":{"lon":72.8479,"lat":19.0144},"weather":[{"id":711,"main":"Smoke","description":"smoke","icon":"50d"}],"base":"stations","main":{"temp":30.99,"feels_like":31.2,"temp_min":27.94,"temp_max":30.99,"pressure":1013,"humidity":42},"visibility":3000,"wind":{"speed":5.66,"deg":280},"clouds":{"all":20},"dt":1710573150,"sys":{"type":1,"id":9052,"country":"IN","sunrise":1710551761,"sunset":1710595109},"timezone":19800,"id":1275339,"name":"Mumbai","cod":200}
// {"coord":{"lon":72.8479,"lat":19.0144},"weather":[{"id":711,"main":"Smoke","description":"smoke","icon":"50d"}],"base":"stations","main":{"temp":30.99,"feels_like":31.2,"temp_min":27.94,"temp_max":30.99,"pressure":1013,"humidity":42},"visibility":3000,"wind":{"speed":5.66,"deg":280},"clouds":{"all":20},"dt":1710573150,"sys":{"type":1,"id":9052,"country":"IN","sunrise":1710551761,"sunset":1710595109},"timezone":19800,"id":1275339,"name":"Mumbai","cod":200}
