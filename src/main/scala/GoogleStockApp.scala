import cats.effect.{IO, IOApp}
import fs2.io.file.{Files, Path}

import java.time.{LocalDate, LocalDateTime}
import scala.util.Try

object GoogleStockApp extends IOApp.Simple {

  case class Record(date: LocalDateTime, status: String, high: String, low: String, close: String, volume: String)

  def toRecord(line: String): Option[Record] = {
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

  override def run: IO[Unit] = {

    // Schema: => Date,Open,High,Low,Close,Volume
    for {

      count <- Files[IO].readUtf8Lines(Path("data/GoogleStockPrices.csv"))
        .tail
        .map(toRecord)
        .filter(_.isDefined)
        .map(_.get)
        .compile
        .count
      _ <- IO.println(count)
    } yield ()

      //.filter(s => !s.trim.isEmpty && !s.startsWith("//"))
      //.map(line => line)
      //.intersperse("\n")
      //.through(text.utf8.encode)
      //.through(Files[IO].writeAll(Path("testdata/celsius.txt")))

  }

}
