import upickle.default.{ macroRW, read as json, ReadWriter as RW }

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
    humidity: Int,
)

case class Coord(lon: Float, lat: Float)

object Coord {
  implicit val rw: RW[Coord] = macroRW
}

case class Weather(id: Int, main: String, description: String)

object Weather {
  implicit val rw: RW[Weather] = macroRW
}

case class Main(temp: Float, pressure: Float, humidity: Int, temp_min: Float, temp_max: Float, feels_like: Float)

object Main {
  implicit val rw: RW[Main] = macroRW
}

case class Wind(speed: Float, deg: Float)

object Wind {
  implicit val rw: RW[Wind] = macroRW
}

case class Clouds(all: Int)

object Clouds {
  implicit val rw: RW[Clouds] = macroRW
}

case class Sys(country: String, sunrise: Int, sunset: Int)

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
    name: String,
)

object WeatherResponse {
  implicit val rw: RW[WeatherResponse] = macroRW
}

case class OpenWeatherBaseCity(id: Int, name: String, lon: Float, lat: Float)

object OpenWeatherBaseCity {
  implicit val rw: RW[OpenWeatherBaseCity] = macroRW
}

// {"coord":{"lon":72.8479,"lat":19.0144},"weather":[{"id":711,"main":"Smoke","description":"smoke","icon":"50d"}],"base":"stations","main":{"temp":30.99,"feels_like":31.2,"temp_min":27.94,"temp_max":30.99,"pressure":1013,"humidity":42},"visibility":3000,"wind":{"speed":5.66,"deg":280},"clouds":{"all":20},"dt":1710573150,"sys":{"type":1,"id":9052,"country":"IN","sunrise":1710551761,"sunset":1710595109},"timezone":19800,"id":1275339,"name":"Mumbai","cod":200}
