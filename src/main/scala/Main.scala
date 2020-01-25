import Configs.{Configs, EncryptedConfig}
import Implicits._
import com.typesafe.config.ConfigFactory
import org.apache.logging.log4j.scala.Logging

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success, Try}


object Main extends Logging {

  def main(args: Array[String]): Unit = {

    val kmsKeyId = Parameter("dw-sftp-to-s3", false).getParameter()

    val job: String = args(0)

    val config: Configs = Configs(ConfigFactory.parseFile(S3EncryptionClient(kmsKeyId).downloadDecryptToFile("configs/application.conf")), job)

    val connector = SftpConnectorImpl(config.sftp)
    val client = S3EncryptionClient(config.aws)
    val decryptionKeys = config.encryption match {
      case encryptionConfig: EncryptedConfig => Some(S3DecryptionKeys(encryptionConfig))
      case _ => None
    }

    val availableFiles = connector.ls()

    val maybeTrimmedFiles =
      config.aws.trimExtension.fold(availableFiles)(
        ext =>
         availableFiles.map(_.stripSuffix(ext))
      )

    val neededFiles: Seq[String] = maybeTrimmedFiles.filterNot(f => client.checkIfExists(f))

    val transferableFiles = config.aws.trimExtension.fold(neededFiles)(
      ext =>
        neededFiles.map(_ ++ ext)
    )

    val bridge = new SftpToS3Bridge(connector, client, decryptionKeys, config)

    val future: Seq[Future[String]] = transferableFiles.map(files => bridge.transfer(files))
    val seqFuture: Future[Seq[Try[String]]] = future.sequenceAsTrys

    seqFuture.map(resultSeq => {
      resultSeq.foreach {
        case Success(_) => Unit
        case Failure(ex) => logger.info(ex)
      }
    })

  }
}
