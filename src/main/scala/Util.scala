import scala.concurrent.ExecutionContext.Implicits.global
import org.apache.logging.log4j.scala.Logging
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

object Util extends Logging{
  /**
    *
    * @param name This is the name of your operation.
    * @param op The operation you would like to time.
    * @tparam T Whatever your operation returns.
    * @return This times how long your operation takes and logs the duration.
    */
  def timeOpAndPrint[T](name: String)(op: => T): T = {
    logger.info(s"Starting $name")
    val before = System.currentTimeMillis()
    val value = op
    val after = System.currentTimeMillis()
    logger.info(s"$name took ${after-before}ms.")
    value
  }
}

object Implicits {

  implicit class RichFuture[T](future: Future[T]) {
    def ofTry: Future[Try[T]] = future.map { t =>
      Success(t)
    }.recover {
      case ex: Throwable =>
        Failure(ex)
    }
  }

  implicit class RichFutureSeq[T](futureTs: Seq[Future[T]]) {
    def sequenceAsTrys: Future[Seq[Try[T]]] =
      Future.sequence(futureTs.map(_.ofTry))
  }

}
