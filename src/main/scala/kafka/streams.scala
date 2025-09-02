package kafka

import cats.effect.kernel.Deferred
import cats.effect.{IO, IOApp, Ref}
import com.comcast.ip4s.{ipv4, port}
import fs2.Chunk
import fs2.kafka.*
import models.kafkaModels.KafkaStats
import org.http4s.HttpRoutes
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.Router
import routes.KafkaStreamServiceControl

import scala.concurrent.duration.DurationInt

object streams {


  
  private val producerSettings = ProducerSettings[IO, String, String]
    .withBootstrapServers("localhost:9092")
    .withEnableIdempotence(true)
    .withRetries(10)

  private val consumerSettings = ConsumerSettings[IO, String, String]
    .withIsolationLevel(IsolationLevel.ReadCommitted)
    .withAutoOffsetReset(AutoOffsetReset.Earliest)
    .withBootstrapServers("localhost:9092")
    .withGroupId("group")

  def producerStream = KafkaProducer
    .stream(producerSettings)
    .flatMap { producer =>
      fs2.Stream.emits(1 to 10000).covary[IO].map { i =>
          ProducerRecords.one(
            ProducerRecord("topic", s"key-$i", s"value-$i")
          )
        }
        .evalMap(producer.produce)
        .groupWithin(100, 3.seconds)
        .evalMap(_.sequence)
      //x => {
//          x.sequence.map(x => {
//            stats.update(x => x.copy(recordsProcessed = x.recordsProcessed + 1))
//            x
//          })
//        })
    }

  private def processRecord(record: ConsumerRecord[String, String],
                            stats: Ref[IO, KafkaStats]): IO[Unit] = {
    stats.update(x => x.copy(recordsProcessed = x.recordsProcessed + 1)) *>
      IO(println(s"Processing record: $record"))
  }
  //.as(CommitNow)

  def interruptableConsumerStream(stopFlag: Ref[IO, Int],
                                  suspendFlag: Ref[IO, Int],
                                  stats: Ref[IO, KafkaStats]): fs2.Stream[IO, Unit] =
    fs2.Stream
      .eval(Deferred[IO, Unit])
      .flatMap { stopSwitch =>

        val switcher = fs2.Stream.eval(
          stopFlag.get.map(x => x >= 1).ifM(
            IO.println("Try to stop") *>
              stopSwitch.complete(()),
            IO.println("Still ON") *>
              stopSwitch.tryGet
          )
        ).repeat.metered(1.second)

        val suspender = fs2.Stream.eval(
          suspendFlag.get.map(x => x > 0)
        ).repeat.metered(1.second)

        val consumerStream =
          KafkaConsumer
            .stream(consumerSettings)
            .subscribeTo("topic")
            .records
            .mapAsync(25) { committable =>
              processRecord(committable.record, stats).as(committable.offset)
            }
            .through(commitBatchWithin(25, 15.seconds))

        consumerStream
          .interruptWhen(stopSwitch.get.attempt)
          .pauseWhen(suspender)
          .concurrently(switcher)
      }


  def switchStream(state: Ref[IO, Int]) = {
    fs2.Stream.eval(state.get)
      .delayBy(5.seconds)
  }
}
