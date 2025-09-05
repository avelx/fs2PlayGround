package services

import cats.effect.IO
import domain.Fruit
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import Fruit._

case class SearchService()  extends Http4sDsl[IO] {

  val fruitsRoute = HttpRoutes.of[IO] {

    case GET -> Root / "fruits" =>
      val fs = Fruit.default
      Ok(fs)
  }
}
