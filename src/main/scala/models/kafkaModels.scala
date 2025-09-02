package models

import io.circe.{Encoder, Json}
import org.http4s.EntityEncoder
import org.http4s.circe.jsonEncoderOf

object kafkaModels {

  case class KafkaStats(recordsProcessed: Int)

  implicit val sessionStateEncoder: Encoder[KafkaStats] = new Encoder[KafkaStats] {
    final def apply(a: KafkaStats): Json = Json.obj(
      ("recordsProcessed", Json.fromInt(a.recordsProcessed)),
    )
  }

  implicit def kafkaStatsEntityEncoder[F[_]]: EntityEncoder[F, KafkaStats] =
    jsonEncoderOf[F, KafkaStats]

}
