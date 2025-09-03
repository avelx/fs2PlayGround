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


object Wikipedia2App extends IOApp.Simple {

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
  // TODO: re-instate categories as its not extracted atm
  case class Categories(element: String)

  case class WikiRecord(id: String,
                        title: String,
                        text: String)

  case class WikiRecordStats(id: String, title: String, frequency: Map[String, Int])

  val conf: Configuration = Configuration()

  override def run: IO[Unit] = {

    /*
      Extract 100 most popular words from the Wiki article:: this would be a simple classification technique
     */
    def processRecord(rec: WikiRecord): WikiRecordStats = {
      val specialChars = List(":", ";", ".", "'", "'")
      val frequency = rec.text.split(" ")
        .map(_.toLowerCase)
        .filter(_.length > 3) // except only words longer than 3 chars
        .filterNot(w => specialChars.exists(c => w.contains(c)))
        .foldLeft(Map[String, Int]()) { (acc, word) =>
          if (acc.contains(word)) {
            acc + (word -> (acc(word) + 1))
          } else {
            acc + (word -> 1)
          }
        }.toList.sortWith((p1, p2) => p1._2 > p2._2) // sort by most occurring word
        .filter(p => p._2 > 3) // actual relevance expect to be often found in the text
        .take(100).toMap
      WikiRecordStats(rec.id, rec.title, frequency)
    }

    def readAllStream(queue: Queue[IO, Option[String]]) = {
      fs2.Stream.fromQueueNoneTerminated(queue)
        .covary[IO]
        .evalMap { sourceFilePath =>
          val name = sourceFilePath.split('/').last
          Logger[IO].info(s"Path: ${sourceFilePath} || File name: $name") *>
            fromParquet[IO]
              .as[WikiRecord]
              .options(ParquetReader.Options(hadoopConf = conf))
              .parallelism(n = 50)
              .read(Path(sourceFilePath))
              .parEvalMap(20) { rec =>
                IO.println(processRecord(rec)) //simple print result
              }.compile.drain
        }
    }

    for {
      queue <- Queue.unbounded[IO, Option[String]]
      wikiStream = readAllStream(queue)
      _ <- {
        os.list(dataSourcePath).toList.map(x => x.toIO.getPath)
          .map(Some(_)) :+ None // None is added at the end to signal Stream to terminate
      }
        .map(x => queue.tryOffer(x)).sequence
      _ <- wikiStream.compile.drain
      //      _ <- Logger[IO].info(s"Total records: ${counts.sum}")
    } yield ()


  }

}