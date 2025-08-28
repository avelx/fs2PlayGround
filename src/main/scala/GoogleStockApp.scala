import cats.effect.*
import cats.effect.kernel.Sync
import config.{AppConfig, PostgreSQLConfig}
import fs2.io.file.{Files, Path}
import models.toRecord
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import resources.AppResources
import resources.Commands.*

object GoogleStockApp extends IOApp.Simple {

  implicit def logger[F[_] : Sync]: Logger[F] = Slf4jLogger.getLogger[F]

  private val maxParallelEvals: Int = 20
  private val appConfig = AppConfig(postgreSQL = PostgreSQLConfig.default)

  override def run: IO[Unit] = {
    AppResources.make[IO](appConfig).use { resource =>
      resource.postgres.use { session =>
        for {
          _ <- Files[IO].readUtf8Lines(Path("data/GoogleStockPrices.csv"))
            .tail // Skip head of the file where we have a schema definition
            .map(toRecord)
            .filter(_.isDefined)
            .collect {
              case Some(record) => record
            }
            .covary[IO]
            .parEvalMap(maxParallelEvals) { rec =>
              IO.blocking( insertStock(session)(rec.toStockRec)) *>
                IO.blocking { // Insert into PostGres and split into few files at the same time
                  val path: os.Path = os.root / "Users" / "pavel" / "devcore" / "Cats-Effects" / "fs2PlayGround" / "data" / s"${rec.date.getYear}.txt"
                  os.write.append(path, s"${rec.toString}\n")
                }
            }
            .compile
            .drain
        } yield ()
      }
    }
  }

}
