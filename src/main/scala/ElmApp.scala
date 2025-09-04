import cats.effect.*
import cats.effect.kernel.Resource
import com.comcast.ip4s.*
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.Server
import org.http4s.server.staticcontent.*
import org.typelevel.log4cats.LoggerFactory
import org.typelevel.log4cats.slf4j.Slf4jFactory

object ElmApp extends IOApp.Simple {

  implicit val loggerFactory: LoggerFactory[IO] = Slf4jFactory.create[IO]

  val app: Resource[IO, Server] =
    EmberServerBuilder
      .default[IO]
      .withHost(ipv4"0.0.0.0")
      .withPort(port"8080")
      .withHttpApp(fileService[IO](FileService.Config("assets")).orNotFound)
      .build

  override def run: IO[Unit] =
    app.use(_ => IO.never)
}
