import java.io.File
import java.nio.file.{Files, Paths}
import Configs.{KeyConfig, PasswordConfig, SftpConfig}
import com.jcraft.jsch._
import org.apache.logging.log4j.scala.Logging
import scala.collection.JavaConverters._
import scala.concurrent.Future
import scala.util.Try
import scala.concurrent.ExecutionContext.Implicits.global



trait SftpConnector {
  def path: String
  def filter: String

  def withChannelAsFuture[T](op: ChannelSftp => Future[T]): Future[T]
}

object SftpConnectorImpl {
  def apply(sftpConfig: SftpConfig): SftpConnectorImpl = sftpConfig match {
    case keyConfig: KeyConfig => SftpConnectorKey(keyConfig)
    case passwordConfig: PasswordConfig => SftpConnectorPassword(passwordConfig)
  }
}

trait SftpConnectorImpl extends SftpConnector with Logging {
  protected def host: String
  protected def username: String
  def path: String
  def filter: String

  protected def createSession(): Session

  def withChannel[T](op: ChannelSftp => T): Try[T] = {
    logger.info(s"Connecting to ${host}")
    val session = createSession()
    session.connect()

    val channel = session.openChannel("sftp").asInstanceOf[ChannelSftp]
    channel.connect()

    val result = Try(op(channel))

    channel.disconnect()
    session.disconnect()

    result
  }

  def withChannelAsFuture[T](op: ChannelSftp => Future[T]): Future[T] = {
    val session = createSession()
    session.connect()

    val channel = session.openChannel("sftp").asInstanceOf[ChannelSftp]
    channel.connect()

    def disconnect = {
      channel.disconnect()
      session.disconnect()
    }

    op(channel).map { result =>
      disconnect
      result
    }.recover {
      case ex: Throwable =>
        disconnect
        throw ex
    }

  }

  def ls(): Seq[String] = {
    withChannel { channel =>
      channel.ls(s"${path}${filter}")
        .asScala
        .map(_.asInstanceOf[channel.LsEntry])
        .map(entry => entry.getFilename)
        .toList
    }.get
  }
}



class SftpConnectorKey(
  val host: String,
  val username: String,
  val key: File,
  val path: String,
  val filter: String
) extends SftpConnectorImpl {

  def createSession(): Session = {
      val jsch = new JSch()
      jsch.addIdentity(key.getAbsolutePath())

      val conf = new java.util.Properties()
      conf.put("StrictHostKeyChecking", "no")

      val session = jsch.getSession(username, host)
      session.setConfig(conf)
      session
  }
}

object SftpConnectorKey {
  def apply(keyConfig: KeyConfig): SftpConnectorKey = {
    val client = S3EncryptionClient(keyConfig.aws)

    val key: File = client.downloadDecryptToFile(keyConfig.keyName)

    key.deleteOnExit()

    new SftpConnectorKey (
      keyConfig.host,
      keyConfig.username,
      key,
      keyConfig.path,
      keyConfig.filter
    )
  }
}


class SftpConnectorPassword(
  val host: String,
  val username: String,
  val password: String,
  val path: String,
  val filter: String
) extends SftpConnectorImpl {

  def createSession(): Session = {
    val jsch = new JSch()
    val conf = new java.util.Properties()
    conf.put("StrictHostKeyChecking", "no")
    val session = jsch.getSession(username, host)
    session.setPassword(password)
    session.setConfig(conf)
    session
  }
}

object SftpConnectorPassword {
  def apply(keyConfig: PasswordConfig): SftpConnectorPassword = {
    new SftpConnectorPassword (
      keyConfig.host,
      keyConfig.username,
      keyConfig.password,
      keyConfig.path,
      keyConfig.filter
    )
  }
}






