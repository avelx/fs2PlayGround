import cats.Id
import cats.effect.std.Queue
import cats.effect.{IO, IOApp, Sync}
import fs2.*

import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import scala.util.Random

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

  val processedHelloWorld = helloWords
    .flatMap(helloWord =>
      goodbyeWords.map(goodbyeWord => s"$helloWord-$goodbyeWord")
    )
    .compile
    .toList

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

  def inc(): Future[Int] = {
    var counter: Int = 0
    def nextValue: Future[Int] = {
      counter += 1
      Future.successful { counter}
    }
    nextValue
  }

  private def queueRefresh(queue: Queue[IO, Int]): IO[Unit] = {
    for {
      _ <- IO.cede
      _ <- IO.sleep(300.millis)
      _ <- queue.tryOffer( Random.nextInt(100) )
      _ <- queueRefresh(queue)
      _ <- IO.cede
    } yield ()
  }

  override def run: IO[Unit] = {
    for {
      queue <- Queue.bounded[IO, Int](50)
      fiber = Stream.fromQueueUnterminated(queue)
        //.debounce(1.seconds)
        .evalMap(i =>
          for {
            _ <- IO.println(s"start $i")
            // Sleep long enough that some debounce periods would be missed
            _ <- IO.sleep(Random.nextInt(1000).millis)
            _ <- IO.println(s"end $i")
            size <- queue.size
            _ <- IO.println(s"QueueSize: ${size}")
          } yield ()
        )
//        .take(2)
        .compile
        .drain
//        .start
//      _ <- queue.offer(1)
//      _ <- queue.offer(2)
      // Sleep 5 seconds so #2 is actively processing
//      _ <- IO.sleep(5.seconds)
//      _ <- queue.offer(3)
      refresh = queueRefresh(queue)
      _ <- IO.racePair(refresh, fiber)
    } yield ()
//    for {
//      _ <- IO.println("Data test ...")
//      res = Stream.eval( {
//      val f = IO(inc())
//        IO.fromFuture(f)
//      }).covary[IO]
//        .repeatN(5).compile.toList
//      x <- res
//      _ <- IO.println(x)
//      //res = processedHelloWorld
//      //_ <- IO.println(res)
//      //res = mostCommonInStream(oneAndFortyTwo)
//      //_ <- IO.println(s"Here is result: ${res}")
//      //_ = infiniteStream.map(x => { println(x); x} ).compile.drain
//      //_ = numbersStream.compile.drain
//      //helloWords_ = emptyAndInfinite.compile.count
//      //_ = zipStream.map(x => { println(x); x }).compile.drain
//    } yield ()
  }

}



