package resources

import cats.effect.IO
import domain.StockPrice
import skunk.{Command, Session}
import skunk.codec.all.{date, float8, varchar}
import skunk.implicits.sql

object Commands {
  
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

  def insertStock(session: Session[IO])(stock: StockPrice): IO[Unit] = {
    session.prepare(insertPriceRecord)
      .flatMap(_.execute(stock))
      .void
  }
}
