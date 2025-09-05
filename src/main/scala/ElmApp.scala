import cats.effect.*
import cats.effect.kernel.Resource
import com.comcast.ip4s.*
import org.http4s.HttpApp
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.{Router, Server}
import org.http4s.server.staticcontent.*
import org.typelevel.log4cats.LoggerFactory
import org.typelevel.log4cats.slf4j.Slf4jFactory
import services.SearchService



object ElmApp extends IOApp.Simple {

  implicit val loggerFactory: LoggerFactory[IO] = Slf4jFactory.create[IO]

  private val searchService: SearchService = SearchService()

  val httpApp: HttpApp[IO] =
    Router(
      "api" -> searchService.fruitsRoute,
      "assets" -> fileService(FileService.Config("./assets"))
    ).orNotFound

  val app: Resource[IO, Server] =
    EmberServerBuilder
      .default[IO]
      .withHost(ipv4"0.0.0.0")
      .withPort(port"8080")
      .withHttpApp(httpApp)
      //.withHttpApp(fileService[IO](FileService.Config("assets")).orNotFound)
      .build

  override def run: IO[Unit] =
    app.use(_ => IO.never)
}
