import cats.effect.{IO, IOApp}
import fs2.*

object Runner extends IOApp.Simple {

  val simpleStream: Stream[Pure, Int] =  Stream
    .range(0, 10)
    .map(x => {
      println(x)
      x
    })

  override def run: IO[Unit] = {
    for {
      _ <- IO.println("Data test ...")
      _ = simpleStream.compile.drain
    } yield ()
  }

}



