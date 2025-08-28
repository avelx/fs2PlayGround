package resources

import models.StockPrice
import skunk.Command
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

}
