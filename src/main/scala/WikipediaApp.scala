import cats.effect.IO.asyncForIO
import cats.effect.kernel.Ref
import cats.effect.std.Queue
import cats.effect.{IO, IOApp}
import cats.syntax.all.*
import com.github.mjakubowski84.parquet4s.parquet.fromParquet
import com.github.mjakubowski84.parquet4s.{ParquetReader, Path}
import config.WikiConfig
import org.apache.hadoop.conf.Configuration
import org.typelevel.log4cats.*
import org.typelevel.log4cats.slf4j.Slf4jLogger


object WikipediaApp extends IOApp.Simple {

  implicit val logger: SelfAwareStructuredLogger[IO] = Slf4jLogger.getLogger[IO]

  val appConfig = WikiConfig.load

  private def savePath(name: String): os.Path = if (appConfig.isProd) {
    os.root / "home" / "pavel" / "data" / "wikipidia" / s"wiki_${name}.txt"
  } else {
    os.root / "Users" / "pavel" / "devcore" / "Cats-Effects" / "fs2PlayGround" / "data" / "wiki" / s"${name}.txt"
  }

  private val dataSourcePath: os.Path = if (appConfig.isProd) {
    os.pwd / "data" / "wikipedia"
  } else {
    os.pwd / "data" / "wiki"
  }

  /*
    Schema inferred from Spark
    |-- id: string (nullable = true)
    |-- title: string (nullable = true)
    |-- text: string (nullable = true)
    |-- categories: array (nullable = true)
    |    |-- element: string (containsNull = true)
   */
  // TODO: re-instate categories
  case class Categories(element: String)

  case class Line(id: String,
                  title: String,
                  text: String)

  val conf: Configuration = Configuration()
  //  conf.set("spark.sql.parquet.enableVectorizedReader", "true")
  //  conf.set("spark.sql.parquet.enableNestedColumnVectorizedReader", "true")

  override def run: IO[Unit] = {

    def readAllStream(queue: Queue[IO, Option[String]], counter: Ref[IO, Int]): fs2.Stream[IO, Long] = {
      fs2.Stream.fromQueueNoneTerminated(queue)
        .covary[IO]
        .evalMap { sourceFilePath =>
          val name = sourceFilePath.split('/').last
          Logger[IO].info(s"Path: ${sourceFilePath} || LastName: $name") *>
            fromParquet[IO]
              .as[Line]
              .options(ParquetReader.Options(hadoopConf = conf))
              .parallelism(n = 50)
              .read(Path(sourceFilePath))
              .parEvalMap(20) { rec =>
                counter.update(c => c + 1) *>
                  counter.get.flatMap { c =>
                    if (c % 1000 == 0) {
                      Logger[IO].info(s"Counter: ${c}") *>
                        IO.pure(c)
                    } else {
                      IO.pure(c)
                    }
                  } *>
                  IO.blocking {
                    val p: os.Path = savePath(name)
                    os.write.append(p, s"${rec.toString}\n")
                  }
              }
              .compile.count
        }
    }

    for {
      counter <- Ref[IO].of(0)
      queue <- Queue.unbounded[IO, Option[String]]
      s = readAllStream(queue, counter)
      _ <- {
        os.list(dataSourcePath).toList.map(x => x.toIO.getPath)
          .map(Some(_)) :+ None
      }
        .map(x => queue.tryOffer(x)).sequence
      counts <- s.compile.toList
      _ <- Logger[IO].info(s"Record number: $counts")
    } yield ()


  }

}