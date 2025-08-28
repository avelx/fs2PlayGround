package models

import java.time.{LocalDate, LocalDateTime}
import scala.util.Try


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

