package routes

import cats.effect.IO
import org.http4s.{EntityEncoder, HttpRoutes}
import cats.effect.*
import io.circe.{Encoder, Json}
import org.http4s.*
import org.http4s.circe.CirceEntityEncoder.circeEntityEncoder
import org.http4s.dsl.io.*
import org.http4s.circe.jsonEncoderOf
import org.http4s.dsl.Http4sDsl

object KafkaRoute {

  case class Tweet(id: Int, message: String)

  implicit val tweetEncoder: Encoder[Tweet] = new Encoder[Tweet] {
    final def apply(a: Tweet): Json = Json.obj(
      ("id", Json.fromInt(a.id)),
      ("message", Json.fromString(a.message))
    )
  }

  implicit def tweetEntityEncoder[F[_]]: EntityEncoder[F, Tweet] =
    jsonEncoderOf[F, Tweet]

  def getTweet: IO[Tweet] = IO(Tweet(1, "test"))

  def getStatus(status: Ref[IO, Int]): IO[Int] = status.get

  def incStatus(status: Ref[IO, Int]): IO[Int] = status.updateAndGet(x => x + 1)

}


case class KafkaService(
                      status: Ref[IO, Int])  extends Http4sDsl[IO] {
  import KafkaRoute._

  val streamControl = HttpRoutes.of[IO] {
    case GET -> Root / "tweet"  =>
      getTweet.flatMap(Ok(_))
    case GET -> Root / "status" =>
      getStatus(status).flatMap { res =>
        Ok(res)
      }
    case GET -> Root / "stop" =>
      incStatus(status).flatMap { res =>
        Ok(res)
      }
  }
}