import java.io.{ByteArrayInputStream, InputStream}

import Configs.Configs
import com.jcraft.jsch.ChannelSftp
import org.mockito.Mockito._
import org.mockito.ArgumentMatchers._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{Matchers, WordSpec}

import scala.concurrent.Future



class SftpToS3BridgeTest extends WordSpec with Matchers with MockitoSugar with ScalaFutures {
  private class NoOpSftpConnector(channel: ChannelSftp) extends SftpConnector {
    override def path: String = "folder/"

    override def filter: String = ".csv"

    override def withChannelAsFuture[T](op: (ChannelSftp) => Future[T]): Future[T] = {
      op(channel)
    }
  }

  def createInputStream(input: String): InputStream = {
    new ByteArrayInputStream(input.getBytes)
  }

  def onGetReturnValidFile(mock: ChannelSftp, stream: InputStream): Unit = {
    when(mock.get(any())).thenReturn(stream)
  }

  def onGetReturnInvalidFile(mock: ChannelSftp, stream: InputStream): Unit = {
    when(mock.get(any())).thenThrow(new Exception())
  }


  def onDecryptReturn(mock: DecryptionKeys, data: DecryptedFileData): Unit = {
    when(mock.decrypt(any())).thenReturn(data)
  }

  def onUploadReturn(mock: S3Client, future: Future[String]): Unit = {
    when(mock.uploadEncryptFromStream(any(), any())).thenReturn(future)
  }

  "The sftp to s3 bridge" when {
    "transfering a file" should {
      "work on an unencrypted file" in {
        val channel = mock[ChannelSftp]
        val sftp = new NoOpSftpConnector(channel)
        val client = mock[S3Client]
        val config = mock[Configs]
        val decryptionKeys = None
        val stream = createInputStream("hello")
        val bridge = new SftpToS3Bridge(sftp, client, decryptionKeys, config)

        onGetReturnValidFile(channel, stream)
        onUploadReturn(client, Future.successful("test"))
        val future = bridge.transfer("test")

        whenReady(future) { filename =>
          verify(channel).get("folder/test")
          verify(client).uploadEncryptFromStream("test", stream)
        }
      }
      "work on an encrypted file" in {
        val file = "test"
        val stream = createInputStream("hello")

        val channel = mock[ChannelSftp]
        val sftp = new NoOpSftpConnector(channel)
        val client = mock[S3Client]
        val config = mock[Configs]

        val decryptionKeys = mock[DecryptionKeys]
        val bridge = new SftpToS3Bridge(sftp, client, Some(decryptionKeys), config)

        onGetReturnValidFile(channel, stream)
        onDecryptReturn(decryptionKeys, DecryptedFileData(file, stream))
        onUploadReturn(client, Future.successful(file))
        val future = bridge.transfer(file)

        whenReady(future) { filename =>
          verify(channel).get(s"folder/$file")
          verify(decryptionKeys).decrypt(EncryptedFileData(file, stream))
          verify(client).uploadEncryptFromStream(file, stream)
        }
      }
    }
  }

}
