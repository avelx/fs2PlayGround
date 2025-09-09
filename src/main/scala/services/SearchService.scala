package services

import cats.effect.IO
import domain.Fruit
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import Fruit.*
import org.http4s.dsl.impl.QueryParamDecoderMatcher

object NameQueryParamMatcher extends QueryParamDecoderMatcher[String]("query")
object SimpleNameQueryParamMatcher extends QueryParamDecoderMatcher[String]("q")


case class SearchService()  extends Http4sDsl[IO] {

  val fruitsRoute = HttpRoutes.of[IO] {

    case GET -> Root / "search" :? NameQueryParamMatcher(q)  =>
      val fs = Fruit.default(q)
      Ok(fs)

    case GET -> Root / "suggest" :? SimpleNameQueryParamMatcher(q)  =>
      val fs = Fruit.getFruitsId(q)
      Ok(fs)

    case GET -> Root / "topItem"  =>
      val fs = Fruit.default("")
      Ok(fs.head)
  }
}
