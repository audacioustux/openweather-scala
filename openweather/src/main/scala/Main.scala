import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer
import upickle.default.{ macroRW, read as json, ReadWriter as RW }

import java.time.format.DateTimeFormatter
import scala.concurrent.Await
import scala.concurrent.Future
import scala.concurrent.duration.Duration
import scala.concurrent.duration.DurationInt
import scala.util.{ Failure, Success }

import akka.Done
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.*
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.kafka.ProducerSettings
import akka.kafka.scaladsl.Producer
import org.apache.kafka.common.serialization.{Deserializer, Serializer}
import upickle.default._
 
@main
def hello(): Unit = {
  given system: ActorSystem[Nothing] = ActorSystem(OpenweatherProducer(), "OpenweatherProducer")
  Await.result(system.whenTerminated, Duration.Inf)
}

object OpenweatherProducer {
  val kfk_bootstrap_server = "localhost:9092"
  val kfk_topic            = "openweather"
  val cities               = Seq("Mumbai", "Hyderabad", "Dhaka")
  val api_key              = sys.env("OPENWEATHER_API_KEY")

  val openweather_endpoint =
    (city: String) => s"http://api.openweathermap.org/data/2.5/weather?q=${ city }&appid=${ api_key }&units=metric"

  // case object Tick
  // def apply() = Behaviors.withTimers { timers =>
  //   timers.startTimerWithFixedDelay(Tick, 10.seconds)
  //   Behaviors.receive { (context, message) =>
  //     val config =
  //       context.system.settings.config.getConfig("akka.kafka.producer")
  //     val producerSettings =
  //       ProducerSettings(config, new StringSerializer, new StringSerializer)
  //         .withBootstrapServers(kfk_bootstrap_server)

  //     given system: ActorSystem[Nothing] = context.system
  //     given ec: scala.concurrent.ExecutionContext = context.executionContext

  //     for (city <- cities) {
  //       val responseFuture: Future[HttpResponse] =
  //         Http()
  //           .singleRequest(
  //             HttpRequest(uri = openweather_endpoint(city))
  //           )

  //       responseFuture
  //         .flatMap { res =>
  //           {
  //             Unmarshal(res).to[String].map { data =>
  //               json[WeatherResponse](data)
  //             }
  //           }
  //         }
  //         .map(weatherResponse => {
  //           val created_at = java.time.LocalDateTime.now()
  //           WeatherData(
  //             created_at = DateTimeFormatter
  //               .ofPattern("yyyy-MM-dd HH:mm:ss")
  //               .format(created_at),
  //             city_id = weatherResponse.id,
  //             city_name = weatherResponse.name,
  //             lat = weatherResponse.coord.lat,
  //             lon = weatherResponse.coord.lon,
  //             country = weatherResponse.sys.country,
  //             temp = weatherResponse.main.temp,
  //             max_temp = weatherResponse.main.temp_max,
  //             min_temp = weatherResponse.main.temp_min,
  //             feels_like = weatherResponse.main.feels_like,
  //             humidity = weatherResponse.main.humidity
  //           )
  //         })
  //         .onComplete {
  //           case Success(value) =>
  //             println(value)
  //           // publish the WeatherData to Kafka topic with Alpakka Kafka

  //           case Failure(e) =>
  //             println(s"An error occurred: $e")
  //         }
  //     }

  //     Behaviors.same
  //   }
  // }

  def apply() = Behaviors.setup { context =>
    given system: ActorSystem[Nothing]          = context.system
    given ec: scala.concurrent.ExecutionContext = context.executionContext

    val source = akka.stream.scaladsl.Source.cycle(() => cities.iterator)
    val flow =
      akka
        .stream
        .scaladsl
        .Flow[String]
        .mapAsync(1) { city =>
          Http()
            .singleRequest(HttpRequest(uri = openweather_endpoint(city)))
            .flatMap { res =>
              Unmarshal(res)
                .to[String]
                .map { data =>
                  json[WeatherResponse](data)
                }
            }
            .map { weatherResponse =>
              val created_at = java.time.LocalDateTime.now()
              WeatherData(
                created_at = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(created_at),
                city_id = weatherResponse.id,
                city_name = weatherResponse.name,
                lat = weatherResponse.coord.lat,
                lon = weatherResponse.coord.lon,
                country = weatherResponse.sys.country,
                temp = weatherResponse.main.temp,
                max_temp = weatherResponse.main.temp_max,
                min_temp = weatherResponse.main.temp_min,
                feels_like = weatherResponse.main.feels_like,
                humidity = weatherResponse.main.humidity,
              )
            }
        }
    
    val weatherDataSerializer: Serializer[WeatherData] = (_: String, data: WeatherData) => {
      writeBinary[WeatherData](data)
    }

    val config = context.system.settings.config.getConfig("akka.kafka.producer")
    val producerSettings = ProducerSettings(config, new StringSerializer, weatherDataSerializer)
      .withBootstrapServers(kfk_bootstrap_server)

    source
      .throttle(3000, 1.day)
      .via(flow)
      .map(value => new ProducerRecord[String, WeatherData](kfk_topic, value))
      .runWith(Producer.plainSink(producerSettings))

    Behaviors.empty
  }

}
