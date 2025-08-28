import cats.effect.IO.asyncForIO
import cats.effect.kernel.Sync
import cats.effect.std.Queue
import cats.effect.{IO, IOApp}
import com.github.mjakubowski84.parquet4s.parquet.fromParquet
import com.github.mjakubowski84.parquet4s.{ParquetReader, Path}
import org.apache.hadoop.conf.Configuration
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

object WikipediaApp extends IOApp.Simple {

  implicit def logger[F[_] : Sync]: Logger[F] = Slf4jLogger.getLogger[F]

  val folder: String = "/Users/pavel/devcore/Cats-Effects/fs2PlayGround/data/wiki/"
  val path: String = s"file:///$folder"

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

    def readAllStream(queue: Queue[IO, Option[String]]): fs2.Stream[IO, Long] = {
      fs2.Stream.fromQueueNoneTerminated(queue)
        //.covary[IO]
        .map(x => {
          println(s"Extract: $x")
          x
        })
        .evalMap { path =>
          fromParquet[IO]
            .as[Line]
            .options(ParquetReader.Options(hadoopConf = conf))
            .parallelism(n = 20)
            .read(Path(path))
            .parEvalMap(10) { rec =>
              IO.blocking {
                val path: os.Path = os.root / "Users" / "pavel" / "devcore" / "Cats-Effects" / "fs2PlayGround" / "data" / "wiki" / "a.txt"
                os.write.append(path, s"${rec.toString}\n")
              }
            }
            .compile.count
        }
    }

    for {
      queue <- Queue.unbounded[IO, Option[String]]
      s = readAllStream(queue)
      _ <-  os.list(os.pwd / "data" / "wiki").toList.map(x => x.toIO.getPath)
            .map(Some(_)).map(x => queue.tryOffer(x)).sequence
      _ <- s.compile.toList
    } yield ()



    //     Topic[IO, String].flatMap { topic =>
    //       val pub = fs2.Stream.eval(os.list(os.pwd / "data" / "wiki").toList)
    //         .map(p => p.toIO.getPath)
    //         .covary[IO].through(topic.publish)
    //       val subscriber = topic.subscribe(10).take(4)
    //       subscriber.concurrently(pub).evalMap(x => x).compile
    //     }.unsafeRunSync()

    //readAllStream(_)

    // p.map(_.toIO.getAbsolutePath)

    //    //
    //    for {
    //      queue <- Queue.unbounded[IO, Option[String]]
    //      paths <- IO.delay(os.list(os.pwd / "data" / "wiki").toList)
    //      _ <- fs2.Stream.fromQueueNoneTerminated(queue)
    //          .readAllStream(path)
    //          .parEvalMap(20) { rec =>
    //          IO.blocking {
    //            val path: os.Path = os.root / "Users" / "pavel" / "devcore" / "Cats-Effects" / "fs2PlayGround" / "data" / "wiki" / "a.txt"
    //            os.write.append(path, s"${rec.toString}\n")
    //          }
    //        }
    //        .compile
    //        .drain
    //    } yield ()

  }

}