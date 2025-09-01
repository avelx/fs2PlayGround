import cats.effect.{IO, IOApp}
import fs2.Chunk
import fs2.kafka.consumer.KafkaConsumeChunk.CommitNow
import fs2.kafka.{AutoOffsetReset, ConsumerRecord, ConsumerSettings, KafkaConsumer, KafkaProducer, ProducerRecord, ProducerRecords, ProducerSettings}

import scala.concurrent.duration.DurationInt

object KafkaSimpleApp extends IOApp.Simple {

  val run: IO[Unit] = {
    val producerSettings = ProducerSettings[IO, String, String].withBootstrapServers("localhost:9092")

    val consumerSettings = ConsumerSettings[IO, String, String]
      .withAutoOffsetReset(AutoOffsetReset.Earliest)
      .withBootstrapServers("localhost:9092")
      .withGroupId("group")

    val producerStream = KafkaProducer
      .stream(producerSettings)
      .flatMap { producer =>
        fs2.Stream.emits(1 to 100).covary[IO].map { i =>
            ProducerRecords.one(
              ProducerRecord("topic", s"key-$i", s"value-$i")
            )
          }
          .evalMap(producer.produce)
          .groupWithin(100, 3.seconds)
          .evalMap(_.sequence)
      }

    def processRecord(record: ConsumerRecord[String, String]): IO[Unit] =
      IO(println(s"Processing record: $record")).as(CommitNow)


    val consumerStream =
      KafkaConsumer
        .stream(consumerSettings)
        .subscribeTo("topic")
        .partitionedRecords
        .map { partitionStream =>
          partitionStream.evalMap { committable =>
            processRecord(committable.record)
          }
        }
        .parJoinUnbounded


    for {
      //_ <- producerStream.covary[IO].compile.drain
      _ <- consumerStream.covary[IO].compile.drain
    } yield ()

  }

}
