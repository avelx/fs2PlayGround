import cats.effect.kernel.Deferred
import cats.effect.{IO, IOApp, Ref}
import com.comcast.ip4s.{ipv4, port}
import fs2.Chunk
import fs2.kafka.*
import org.http4s.HttpRoutes
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.Router
import routes.KafkaService

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
        fs2.Stream.emits(1 to 10000).covary[IO].map { i =>
            ProducerRecords.one(
              ProducerRecord("topic", s"key-$i", s"value-$i")
            )
          }
          .evalMap(producer.produce)
          .groupWithin(100, 3.seconds)
          .evalMap(_.sequence)
      }

    def processRecord(record: ConsumerRecord[String, String]): IO[Unit] =
      IO(println(s"Processing record: $record")) //.as(CommitNow)

    def interpretableConsumerStream(state: Ref[IO, Int]) = fs2.Stream.eval(Deferred[IO, Unit])
      .flatMap { switch =>

        val switcher =
          fs2.Stream.eval(
            state.get.map(x => x >= 1).ifM(
              IO.println("Try to stop") *>
                switch.complete(()),
              IO.println("Still ON") *>
                switch.tryGet
            )
          ).repeat.metered(1.second)

        val consumerStream =
          KafkaConsumer
            .stream(consumerSettings)
            .subscribeTo("topic")
            .records
            .mapAsync(25) { committable =>
              processRecord(committable.record).as(committable.offset)
            }
            .through(commitBatchWithin(25, 15.seconds))

        consumerStream
          .interruptWhen(switch.get.attempt)
          .concurrently(switcher)
      }

    def switchStream(state: Ref[IO, Int]) = {
      fs2.Stream.eval(state.get)
        .delayBy(5.seconds)
    }

    for {
      status <- Ref[IO].of(0)
      routes = KafkaService(status)
      services = routes.streamControl
      httpApp = Router("/api" -> services).orNotFound
      ember = EmberServerBuilder
        .default[IO]
        .withHost(ipv4"0.0.0.0")
        .withPort(port"8080")
        .withHttpApp(httpApp)
        .build
        .use(_ => IO.never)
      consumer = interpretableConsumerStream(status).compile.drain
      producer = producerStream.covary[IO].compile.drain *> IO.never
      _ <- IO.racePair( IO.racePair(ember, consumer), producer)
    } yield ()

  }

}
