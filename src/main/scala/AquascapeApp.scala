import aquascape.*
import cats.effect.*
import fs2.*

object App extends AquascapeApp {

  def name: String = "aquascapeFrame" // The name of the png file (Scala) or HTML frame id (Scala.js)

  def stream(using Scape[IO]): IO[Unit] = {
    Stream(1, 2, 3)
      .stage("Stream(1, 2, 3)")       // `stage` introduces a stage.
      .evalMap(x => IO(x).trace())    // `trace` traces a side effect.
      .stage("evalMap(…)")
      .compile
      .toList
      .compileStage("compile.toList") // `compileStage` is used for the final stage.
      .void
  }
}