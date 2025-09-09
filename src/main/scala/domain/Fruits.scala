package domain

import io.circe.{Encoder, Json}
import org.http4s.EntityEncoder
import org.http4s.circe.jsonEncoderOf

final case class Fruit(name: String, desc: String)
final case class FruitId(id: Int, name: String)

object Fruit {

  implicit val fruitEncoder: Encoder[Fruit] = new Encoder[Fruit] {
    final def apply(f: Fruit): Json = Json.obj(
      ("name", Json.fromString(f.name)),
      ("description", Json.fromString(f.desc))
    )
  }

  implicit val fruitIdEncoder: Encoder[FruitId] = new Encoder[FruitId] {
    final def apply(f: FruitId): Json = Json.obj(
      ("name", Json.fromString(f.name)),
      ("id", Json.fromInt(f.id))
    )
  }

  implicit def fruitIdEntityEncoder[F[_]]: EntityEncoder[F, FruitId] = {
    jsonEncoderOf[F, FruitId]
  }
  
  implicit def fruitEntityEncoder[F[_]]: EntityEncoder[F, Fruit] = {
    jsonEncoderOf[F, Fruit]
  }

  implicit def fruitsEntityEncoder[F[_]]: EntityEncoder[F, Seq[Fruit]] = {
    jsonEncoderOf[F, Seq[Fruit]]
  }

  implicit def fruitsIdEntityEncoder[F[_]]: EntityEncoder[F, Seq[FruitId]] = {
    jsonEncoderOf[F, Seq[FruitId]]
  }

  def default(query: String): Seq[Fruit] = {
    List(
      Fruit("apple", "A sweet fruit"),
      Fruit("ananas", "A sweet fruit"),
      Fruit("banana", "A yellow fruit"),
      Fruit("orange", "A citrus fruit"),
      Fruit("apricot", "A small orange fruit"),
      Fruit("avocado", "A creamy green fruit")
    ).filter(rec => rec.name.contains(query))
  }

  def getFruitsId(query: String): Seq[FruitId] = {
    List(
      FruitId(1, "apple"),
      FruitId(2, "ananas"),
      FruitId(3, "banana"),
      FruitId(4, "orange"),
      FruitId(5, "apricot" ),
      FruitId(6, "avocado")
    ).filter(rec => rec.name.contains(query))
  }
}
