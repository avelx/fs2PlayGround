import cats.Id
import cats.effect.{IO, IOApp}
import fs2.*

object Runner extends IOApp.Simple {

  val simpleStream: Id[Option[Int]] =  Stream
    .range(0, 10)
    .fold(0){ (acc, current) =>
      acc + current
    }.compile.last

  val numbersStream: Stream[Pure, Int] = Stream.range(0, Int.MaxValue/ 10)

  val emptyAndInfinite = Stream.empty

  val helloWords = Stream("Hello", "Cádiz")
  val goodbyeWords = Stream("Goodbye", "London", "Test")
  val goodbyeWords2 = Stream("Data", "Test", "ADD")

  /** ++ is an alias for append */
  val append = (helloWords ++ goodbyeWords).compile.toList

  val zipStream = helloWords.zip(goodbyeWords).zip(goodbyeWords2)

  def mostCommonInStream(ids: Stream[Pure, String]): String = {
    val initialCounts: Map[String, Int] = Map.empty
    val finalCounts = ids
      .fold(initialCounts) { (counts, id) =>
        println(s"Item: $id")
        val previousCount = counts.getOrElse(id, 0)
        counts + ((id, previousCount + 1))
      }
      .compile
      .last
      .getOrElse(initialCounts)
    val (mostCommonId, _) = finalCounts.maxBy { case (_, count) => count }
    mostCommonId
  }

  val oneAndFortyTwo: Stream[Pure, String] = Stream("u42", "u1", "u42").repeatN(5)

  val infiniteStream = Stream(1).repeat

  override def run: IO[Unit] = {
    for {
      _ <- IO.println("Data test ...")
      //res = mostCommonInStream(oneAndFortyTwo)
      //_ <- IO.println(s"Here is result: ${res}")
      //_ = infiniteStream.map(x => { println(x); x} ).compile.drain
      //_ = numbersStream.compile.drain
      //helloWords_ = emptyAndInfinite.compile.count
      //_ = zipStream.map(x => { println(x); x }).compile.drain
    } yield ()
  }

}



