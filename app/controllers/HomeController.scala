package controllers

import javax.inject._
import play.api._
import play.api.mvc._
import akka.stream.alpakka.elasticsearch.ElasticsearchConnectionSettings
import akka.stream.alpakka.elasticsearch.scaladsl.ElasticsearchSource
import akka.stream.alpakka.elasticsearch.ElasticsearchParams
import akka.stream.alpakka.elasticsearch.ElasticsearchSourceSettings
import akka.stream.scaladsl.Sink
import spray.json.DefaultJsonProtocol._
import spray.json._

case class WeatherData(
    city: String,
    temperature: Double,
    humidity: Double,
    pressure: Double
)
object JsonFormats {
  implicit val movieFormat: JsonFormat[WeatherData] = jsonFormat4(WeatherData)
}

/** This controller creates an `Action` to handle HTTP requests to the
  * application's home page.
  */
@Singleton
class HomeController @Inject() (val controllerComponents: ControllerComponents)
    extends BaseController {

  val logger = play.api.Logger(getClass)

  /** Create an Action to render an HTML page.
    *
    * The configuration in the `routes` file means that this method will be
    * called when the application receives a `GET` request with a path of `/`.
    */
  def index() = Action { implicit request: Request[AnyContent] =>
    // val connectionSettings =
    //   ElasticsearchConnectionSettings("http://localhost:9200")

    // import JsonFormats._
    // val reading = ElasticsearchSource
    //   .typed[WeatherData](
    //     ElasticsearchParams.V7("openweather"),
    //     """{"match_all": {}}""",
    //     ElasticsearchSourceSettings(connectionSettings)
    //   )
    //   .map(_.source)
    //   .runWith(Sink.seq)
    // reading.foreach(_ => log.info("Reading finished"))(
    //   actorSystem.executionContext
    // )

    Ok(views.html.index())
  }
}
