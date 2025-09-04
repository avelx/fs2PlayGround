import cats.effect.{IO, IOApp}
import config.WikiConfig
import org.typelevel.log4cats.SelfAwareStructuredLogger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import processor.TextProcessor._

object StanfordApp extends IOApp.Simple {

  implicit val logger: SelfAwareStructuredLogger[IO] = Slf4jLogger.getLogger[IO]

  val appConfig = WikiConfig.load

  private val dataSourcePath: os.Path = {
    os.pwd / "data" / "wiki-samples" / "165709.txt"
  }

  def readFile(path: os.Path): String = {
    os.read.lines(path).mkString("")
  }

  override def run: IO[Unit] = {
    for {
      text <- IO.blocking(readFile(dataSourcePath))
      _ <- IO.blocking(exec(text))
    } yield ()
  }
}
