package routes

import cats.effect.*
import io.circe.Encoder
import org.http4s.*
import org.http4s.circe.CirceEntityEncoder.circeEntityEncoder
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.io.*

object KafkaRoute {
  def getStatus(status: Ref[IO, Int]): IO[Int] = status.get
  def tryStop(status: Ref[IO, Int]): IO[Int] = status.updateAndGet(x => x + 1)
}


case class KafkaStreamServiceControl(
                      status: Ref[IO, Int])  extends Http4sDsl[IO] {
  import KafkaRoute.*

  val streamControl = HttpRoutes.of[IO] {
    case GET -> Root / "status" =>
      getStatus(status).flatMap { res =>
        Ok(res)
      }
    case GET -> Root / "stop" =>
      tryStop(status).flatMap { res =>
        Ok(res)
      }
  }
}