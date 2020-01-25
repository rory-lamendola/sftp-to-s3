import Configs.{AWSConfig, Configs}
import java.io.{File, InputStream}
import java.nio.file.{Files, Paths}

import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.AmazonS3Encryption
import com.amazonaws.services.s3.AmazonS3EncryptionClientBuilder
import com.amazonaws.services.s3.model._
import org.apache.logging.log4j.scala.Logging

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Try


trait S3Client extends Logging {
  def client: AmazonS3Encryption
  def bucket: String
  def job: String

  def uploadEncryptFromFile(fileName: String, file: File): String

  def uploadEncryptFromStream(fileName: String, inputStream: InputStream): Future[String]

  def downloadDecrypt(fileName: String): Try[S3Object]

  def downloadDecryptToFile(fileName: String): File

  def checkIfExists(fileName: String): Boolean
}




class S3EncryptionClient(
  override val client: AmazonS3Encryption,
  override val bucket: String,
  override val job: String
) extends S3Client {

  def uploadEncryptFromFile(fileName: String, file: File): String = {
    Util.timeOpAndPrint(s"Upload of s3://$bucket/raw/$job/$fileName") {
      val putObjectRequest = new PutObjectRequest(bucket, s"raw/$job/$fileName", file)
      client.putObject(putObjectRequest)
      fileName
    }
  }

  def uploadEncryptFromStream(fileName: String, inputStream: InputStream): Future[String] = {
    logger.info(s"Creating temp file $fileName")
    val file = new File(fileName)
    file.deleteOnExit()
    val path = Paths.get(file.getAbsolutePath)
    Future {
      logger.info(s"Copying data into $fileName")
      Files.copy(inputStream, path)
      uploadEncryptFromFile(fileName, file)
      fileName
    }.recover {
      case ex: Throwable =>
        throw new Exception(s"Failed to upload $fileName.", ex)
    }
  }

  def downloadDecrypt(fileName: String): Try[S3Object] = {
    Try {
      Util.timeOpAndPrint(s"Downloads of s3://$bucket/$fileName") {
        client.getObject(bucket, fileName)
      }
    }
  }

  def downloadDecryptToFile(fileName: String): File = {
    val baseFileName = fileName.split('/').last
    val out = new File(s"/tmp/$baseFileName")
    out.deleteOnExit()
    logger.info(s"downloading $fileName")
    val s3Object = downloadDecrypt(fileName)
    if (s3Object.isSuccess) {
      logger.info(s"Downloaded $fileName to /tmp/$baseFileName")
      Files.copy(s3Object.get.getObjectContent, Paths.get(out.getAbsolutePath()))
      out
    } else {
      logger.error(s"Unable to download $fileName to file")
      out
    }
  }

  def checkIfExists(fileName: String): Boolean = {
    logger.info(s"checking if $fileName exists in s3://$bucket/raw/$job or s3://$bucket/processed/$job")
    val exists = client.doesObjectExist(bucket, s"raw/$job/$fileName") || client.doesObjectExist(bucket, s"processed/$job/$fileName")

    if(exists) logger.info(s"$fileName already exists in s3://$bucket/raw/$job or s3://$bucket/processed/$job")
    else logger.info(s"$fileName does not exist in s3://$bucket/raw/$job or s3://$bucket/processed/$job and needs to be retrieved")

    exists
  }
}

object S3EncryptionClient {
  def apply(config: AWSConfig): S3EncryptionClient = {
    val materialProvider = new KMSEncryptionMaterialsProvider(config.kms)
    val client = AmazonS3EncryptionClientBuilder.standard()
      .withRegion(Regions.US_EAST_1)
      .withCryptoConfiguration(new CryptoConfiguration(CryptoMode.EncryptionOnly))
      .withEncryptionMaterials(materialProvider).build()

    new S3EncryptionClient(client, config.name, config.job)

  }

  def apply(kms: String): S3EncryptionClient = {
    val materialProvider = new KMSEncryptionMaterialsProvider(kms)
    val client = AmazonS3EncryptionClientBuilder.standard()
      .withRegion(Regions.US_EAST_1)
      .withCryptoConfiguration(new CryptoConfiguration(CryptoMode.EncryptionOnly))
      .withEncryptionMaterials(materialProvider).build()

    new S3EncryptionClient(client, "dw-sftp-to-s3", "default")

  }
}


class SftpToS3Bridge(
  val sftp: SftpConnector,
  val client: S3Client,
  val decryptionKeysOpt: Option[DecryptionKeys],
  val config : Configs
) {

  def transfer(filename: String): Future[String] = {
    sftp.withChannelAsFuture { channel =>
      val stream = channel.get(s"${sftp.path}$filename")
      val decrypted = decryptionKeysOpt match {
        case None => DecryptedFileData(filename, stream)
        case Some(keys) => keys.decrypt(EncryptedFileData(filename, stream))
      }
      val awsFilename =
        config.aws.trimExtension.fold(decrypted.filename)(ext => decrypted.filename.stripSuffix(ext))
      client.uploadEncryptFromStream(awsFilename, decrypted.contents)
    }
  }
}


