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

  val isProd : Boolean = false

  private def savePath(name: String): os.Path = if (isProd) {
    os.root / "home" / "pavel" / "data" / "wikipidia" / s"wiki_${name}.txt"
  } else {
    os.root / "Users" / "pavel" / "devcore" / "Cats-Effects" / "fs2PlayGround" / "data" / "wiki" / s"${name}.txt"
  }

  private val dataSourcePath: os.Path = if (isProd) {
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

    def readAllStream(queue: Queue[IO, Option[String]]): fs2.Stream[IO, Long] = {
      fs2.Stream.fromQueueNoneTerminated(queue)
        //.covary[IO]
        .map(x => {
          println(s"Extract: $x")
          x
        })
        .evalMap { sourceFilePath =>
          val name = sourceFilePath.split('/').last
          println(s"LastName: $name")
          fromParquet[IO]
            .as[Line]
            .options(ParquetReader.Options(hadoopConf = conf))
            .parallelism(n = 20)
            .read(Path(sourceFilePath))
            .parEvalMap(10) { rec =>
              IO.blocking {
                val p: os.Path = savePath(name)
                if (p.toIO.exists()) {
                  os.write.append(p, s"${rec.toString}\n")
                } else {
                  os.write(p, s"${rec.toString}\n")
                }
              }
            }
            .compile.count
        }
    }

    for {
      queue <- Queue.unbounded[IO, Option[String]]
      s = readAllStream(queue)
      _ <-  os.list(dataSourcePath).toList.map(x => x.toIO.getPath)
            .map(Some(_)).map(x => queue.tryOffer(x)).sequence
      _ <- s.compile.toList
    } yield ()


  }

}