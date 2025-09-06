package domain

import io.circe.{Encoder, Json}
import org.http4s.EntityEncoder
import org.http4s.circe.jsonEncoderOf

/*
object Fruit {
    [
    ( "apple", "A sweet fruit" )
    , ( "banana", "A yellow fruit" )
    , ( "orange", "A citrus fruit" )
    , ( "apricot", "A small orange fruit" )
    , ( "avocado", "A creamy green fruit" )
    ]
 */

final case class Fruit(name: String) //, desc: String)

object Fruit {

  implicit val fruitEncoder: Encoder[Fruit] = new Encoder[Fruit] {
    final def apply(f: Fruit): Json = Json.obj(
      ("name", Json.fromString(f.name))
//      ("description", Json.fromString(f.desc))
    )
  }

  implicit def fruitEntityEncoder[F[_]]: EntityEncoder[F, Fruit] = {
    jsonEncoderOf[F, Fruit]
  }

  implicit def fruitsEntityEncoder[F[_]]: EntityEncoder[F, Seq[Fruit]] = {
    jsonEncoderOf[F, Seq[Fruit]]
  }

  def default(query: String): Seq[Fruit] = {
    List(
      Fruit("apple"),  //"A sweet fruit"),
      Fruit("banana"), //"A yellow fruit"),
      Fruit("orange"), //"A citrus fruit"),
      Fruit("apricot"), //"A small orange fruit"),
      Fruit("avocado"), //"A creamy green fruit")
    )
      //.filter(rec => rec.name.contains(query))
  }
}
