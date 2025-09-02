package routes

import cats.effect.*
import io.circe.Encoder
import models.kafkaModels.KafkaStats
import org.http4s.*
import org.http4s.circe.CirceEntityEncoder.circeEntityEncoder
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.io.*

object KafkaRoute {
  def getStatus(status: Ref[IO, KafkaStats]): IO[KafkaStats] = status.get
  def tryStop(status: Ref[IO, Int]): IO[Int] = status.updateAndGet(x => x + 1)
  def trySuspend(suspendFlag: Ref[IO, Int]): IO[Int] = suspendFlag.updateAndGet(x => x + 1)
  def tryRestart(suspendFlag: Ref[IO, Int]): IO[Int] = suspendFlag.updateAndGet(x => x - 1)
}


case class KafkaStreamServiceControl(
                                      stopFlag: Ref[IO, Int],
                                      suspendFlag: Ref[IO, Int],
                                      stats: Ref[IO, KafkaStats])  extends Http4sDsl[IO] {
  import KafkaRoute.*

  val streamControl = HttpRoutes.of[IO] {
    case GET -> Root / "status" =>
      getStatus(stats).flatMap { stats =>
        Ok(stats)
      }
    case GET -> Root / "stop" =>
      tryStop(stopFlag).flatMap { res =>
        Ok(res)
      }
    case GET -> Root / "suspend" =>
      trySuspend(suspendFlag).flatMap { res =>
        Ok(res)
      }

    case GET -> Root / "restart" =>
      tryRestart(suspendFlag).flatMap { res =>
        Ok(res)
      }
  }
}