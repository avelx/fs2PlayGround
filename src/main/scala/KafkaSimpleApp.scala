import cats.effect.{IO, IOApp}
import fs2.kafka.{AutoOffsetReset, ConsumerSettings, KafkaConsumer, KafkaProducer, ProducerRecord, ProducerRecords, ProducerSettings}

import scala.concurrent.duration.DurationInt

object KafkaSimpleApp extends IOApp.Simple {

  val run: IO[Unit] = {
    val producerSettings =ProducerSettings[IO, String, String].withBootstrapServers("localhost:9092")

    val stream =
      KafkaProducer
        .stream(producerSettings)
        .flatMap { producer =>
          fs2.Stream.emits(1 to 10).covary[IO].map { i =>
              ProducerRecords.one(
                            ProducerRecord("topic", s"key-$i", s"value-$i")
                          )
                        }
            .evalMap(producer.produce)
            .groupWithin(500, 15.seconds)
            .evalMap(_.sequence)
        }


    for {
      _ <- stream.compile.drain
    } yield ()

  }

}
