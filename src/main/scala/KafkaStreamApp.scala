import cats.effect.{IO, IOApp, Ref}
import com.comcast.ip4s.{ipv4, port}
import kafka.streams.{interpretableConsumerStream, producerStream}
import org.http4s.HttpRoutes
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.Router
import routes.KafkaStreamServiceControl

object KafkaStreamApp extends IOApp.Simple {

  val run: IO[Unit] = {
    for {
      status <- Ref[IO].of(0)
      routes = KafkaStreamServiceControl(status)
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
      _ <- IO.racePair(IO.racePair(ember, consumer), producer)
    } yield ()

  }

}
