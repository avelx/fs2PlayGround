import cats.effect.kernel.Sync
import cats.effect.{IO, IOApp}
import com.github.mjakubowski84.parquet4s.{ParquetReader, Path}
import com.github.mjakubowski84.parquet4s.parquet.fromParquet
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import org.apache.hadoop.conf.Configuration


object WikipediaApp extends IOApp.Simple {

  implicit def logger[F[_] : Sync]: Logger[F] = Slf4jLogger.getLogger[F]

  val path: String = "file:///Users/pavel/devcore/Cats-Effects/fs2PlayGround/data/wiki/a.parquet"

  /*
    Schema inferred from Spark
    |-- id: string (nullable = true)
    |-- title: string (nullable = true)
    |-- text: string (nullable = true)
    |-- categories: array (nullable = true)
    |    |-- element: string (containsNull = true)
   */
  case class Categories(element: String)

  case class Line(id: String,
                  title:String,
                  text: String)

  val conf: Configuration = Configuration()
  conf.set("spark.sql.parquet.enableVectorizedReader", "true")
  conf.set("spark.sql.parquet.enableNestedColumnVectorizedReader", "true")

  override def run: IO[Unit] = {

    val readAllStream = fromParquet[IO]
      .as[Line]
      .options(ParquetReader.Options(hadoopConf = conf))
      .parallelism(n = 4)
      .read(Path(path))
      .printlns

    for {
      _ <- readAllStream
        .compile
        .drain
    } yield ()
  }

}