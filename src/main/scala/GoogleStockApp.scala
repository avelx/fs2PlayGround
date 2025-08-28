import cats.effect.*
import cats.effect.kernel.Sync
import config.{AppConfig, PostgreSQLConfig}
import fs2.io.file.{Files, Path}
import models.{StockPrice, toRecord}
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import resources.AppResources
import resources.Commands.insertPriceRecord
import skunk.*


object GoogleStockApp extends IOApp.Simple {

  implicit def logger[F[_] : Sync]: Logger[F] = Slf4jLogger.getLogger[F]

  private val maxParallelEvals: Int = 20
  private val appConfig = AppConfig(postgreSQL = PostgreSQLConfig.default)

  def runInsertFilm(appConfig: AppConfig, insertFilm: List[(String, String)] => Command[List[(String, String)]]): IO[Unit] = {
    val pairs = List[(String, String)](("3", "Bob"))
    val insertPairs = insertFilm(pairs)
    for {
      _ <- createPostgresResource(appConfig).use { res =>
        res.postgres.use { session =>
          session.prepare(insertPairs).flatMap { cmd =>
            cmd.execute(pairs)
          }
        }
      }
    } yield ()
  }

  def insertStock(stock: StockPrice): IO[Unit] = {
    for {
      _ <- createPostgresResource(appConfig).use { res =>
        res.postgres.use { session =>
          session.prepare(insertPriceRecord)
            .flatMap(_.execute(stock)).void
        }
      }
    } yield ()
  }

  def createPostgresResource(appConf: AppConfig): Resource[IO, AppResources[IO]] = {
    AppResources.make[IO](appConf)
  }

  override def run: IO[Unit] = {
    
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
          IO.blocking(insertStock(rec.toStockRec)) *>
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
