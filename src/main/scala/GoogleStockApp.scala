import cats.effect.{IO, IOApp}
import fs2.io.file.{Files, Path}

import java.time.{LocalDateTime}
import scala.util.Try

object GoogleStockApp extends IOApp.Simple {

  private case class Record(date: LocalDateTime,
                            status: String,
                            high: String,
                            low: String,
                            close: String,
                            volume: String) {
    override def toString: String = {
      s"$date;$status;$high;$low;$low;$close;$volume"
    }
  }

  private def toRecord(line: String): Option[Record] = {
    val arr = line.split(",")
    Try{
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

  //def asStream(in: Record): fs2.Stream[IO, Record] = ???

  override def run: IO[Unit] = {

    // Schema: => Date,Open,High,Low,Close,Volume
    for {
      _ <- Files[IO].readUtf8Lines(Path("data/GoogleStockPrices.csv"))
        .tail
        .map(toRecord)
        .filter(_.isDefined)
        .collect{
          case Some(record) =>
            (record.date.toLocalDate.getYear.toString, record.toString)
        }
        .covary[IO] // Set maxConcurrency to 5
        .parEvalMap(5) { rec =>
          IO.delay {
            val path: os.Path = os.root / "Users" / "pavel" / "devcore" / "Cats-Effects" / "fs2PlayGround" / "data" / s"${rec._1}.txt"
            os.write.append(path, s"${rec._2}\n")
          }
        }
        .compile
        .drain
    } yield ()
  }

}
