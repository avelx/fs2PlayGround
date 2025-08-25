import aquascape.*
import cats.effect.*
import fs2.*

import scala.concurrent.duration.DurationInt

object App extends AquascapeApp {

  def name: String = "aquascapeFrame" // The name of the png file (Scala) or HTML frame id (Scala.js)

  def stream(using Scape[IO]): IO[Unit] = {
//    Stream(1, 2, 3)
//      .stage("Stream(1, 2, 3)")       // `stage` introduces a stage.
//      .evalMap(x => IO(x).trace())    // `trace` traces a side effect.
//      .stage("evalMap(…)")
//      .compile
//      .toList
    Stream('a', 'b', 'c')
      .stage("Stream(a, b, c)")
      .evalMap(x => IO(x).trace())
      .parEvalMapUnbounded(ch => IO.sleep(1.second).as(ch))
      .compile
      .toList
      .compileStage("compile.toList") // `compileStage` is used for the final stage.
      .void
  }
}