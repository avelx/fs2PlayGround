import cats.effect.kernel.Sync
import cats.effect.{IO, IOApp}
import config.{AppConfig, PostgreSQLConfig}
import fs2.io.file.{Files, Path}
import resources.AppResources

import java.time.{LocalDate, LocalDateTime}
import scala.util.Try
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import cats.effect.*
import cats.syntax.all.*
import skunk.*
import skunk.codec.all.*
import skunk.implicits.*


object GoogleStockApp extends IOApp.Simple {

  implicit def logger[F[_] : Sync]: Logger[F] = Slf4jLogger.getLogger[F]

  override def run: IO[Unit] = {
    // Schema: => Date,Open,High,Low,Close,Volume
    val appConfig = AppConfig(
      PostgreSQLConfig(
        host = "localhost",
        user = "postgres",
        password = "my-password",
        database = "store",
        port = 5432,
        max = 5
      )
    )

    case class Film(code: String, title: String)
    case class StockPrice(company: String, trade_date: LocalDate, priceOpen: Double)

    case class Record(date: LocalDateTime,
                      status: String,
                      high: String,
                      low: String,
                      close: String,
                      volume: String) {
      override def toString: String = {
        s"$date;$status;$high;$low;$low;$close;$volume"
      }

      def toStockRec: StockPrice = {
        StockPrice("Google", this.date.toLocalDate, this.close.toDouble)
      }
    }

    def toRecord(line: String): Option[Record] = {
      val arr = line.split(",")
      Try {
        Record(
          LocalDateTime.parse(arr(0).replace(" ", "T")),
          arr(1),
          arr(2),
          arr(3),
          arr(4),
          arr(5)
        )
      }.toEither match {
        case Right(record) =>
          //println(record)
          Some(record)
        case Left(ex) =>
          //println(ex)
          // TODO: log error
          None
      }
    }


    def insertFilm(vals: List[(String, String)]): Command[vals.type] = {
      val enc = (varchar ~ varchar).values.list(vals)
      sql"""
          INSERT INTO films
          VALUES $enc""".command
    }

    val insertPriceRecord: Command[StockPrice] = {
      sql"""
            INSERT INTO StockPrice (company_name, trade_date, price_open) VALUES ($varchar, $date, $float8) """
        .command
        .to[StockPrice]
    }

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

    //runInsertFilm(appConfig, insertFilm _)

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

    for {
      _ <- Files[IO].readUtf8Lines(Path("data/GoogleStockPrices.csv"))
        .tail
        .map(toRecord)
        .filter(_.isDefined)
        .collect {
          case Some(record) => record
        }
        .map(x => x)
        .covary[IO] // Set maxConcurrency to 5
        //        .map(rec => StockPrice("Google", rec._1.))
        .parEvalMap(5) { rec =>
          insertStock(rec.toStockRec) *>
            IO.delay { // Insert into PostGres and split into few files at the same time
              val path: os.Path = os.root / "Users" / "pavel" / "devcore" / "Cats-Effects" / "fs2PlayGround" / "data" / s"${rec.date.getYear}.txt"
              os.write.append(path, s"${rec.toString}\n")
            }
        }
        .compile
        .drain
    } yield ()


  }


  //def asStream(in: Record): fs2.Stream[IO, Record] = ???

  def createPostgresResource(appConf: AppConfig): Resource[IO, AppResources[IO]] = {
    AppResources.make[IO](appConf)
  }

  //  val tapResource = Resource.make(
  //    IO.println("Opening tap.").as(Tap("Hot"))
  //  )(_
  //  => IO.println("Closing tap."))


}
