import Configs.{EncryptedConfig}
import java.io.{File, InputStream}
import java.security.Security
import org.bouncycastle.jce.provider.BouncyCastleProvider
import name.neuhalfen.projects.crypto.bouncycastle.openpgp.BouncyGPG
import name.neuhalfen.projects.crypto.bouncycastle.openpgp.keys.callbacks.KeyringConfigCallbacks
import name.neuhalfen.projects.crypto.bouncycastle.openpgp.keys.keyrings.KeyringConfigs
import org.apache.logging.log4j.scala.Logging


trait FileData {
  def filename: String
  def contents: InputStream
}

case class EncryptedFileData(
  filename: String,
  contents: InputStream,
) extends FileData

case class DecryptedFileData(
  filename: String,
  contents: InputStream
) extends FileData


trait DecryptionKeys extends Logging{

  //In order to use the OpenPGP library, we need to register a bouncy castle provider as listed in the HOWTO of this library: https://github.com/neuhalje/bouncy-gpg
  if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
    Security.addProvider(new BouncyCastleProvider)
  }

  protected def publicKey: File
  protected def privateKey: File
  protected def password: String


  def decrypt(data: EncryptedFileData): DecryptedFileData = {
    Util.timeOpAndPrint(s"Decrypting ${data.filename}") {
      val keyRingConfig = KeyringConfigs.withKeyRingsFromFiles(
        publicKey,
        privateKey,
        KeyringConfigCallbacks.withPassword(password)
      )

      val decryptedMessage = BouncyGPG.decryptAndVerifyStream
        .withConfig(keyRingConfig)
        .andIgnoreSignatures()
        .fromEncryptedInputStream(data.contents)

      DecryptedFileData(data.filename, decryptedMessage)
    }
  }
}

object S3DecryptionKeys {
  def apply(encryptedConfig: EncryptedConfig): DecryptionKeys = {
    val client = S3EncryptionClient(encryptedConfig.aws)

    new DecryptionKeys {
      logger.info(s"Getting public key")
      override val publicKey: File = client.downloadDecryptToFile(encryptedConfig.publicKeyFile)
      publicKey.deleteOnExit()

      logger.info(s"Getting private key")
      override val privateKey: File = client.downloadDecryptToFile(encryptedConfig.privateKeyFile)
      privateKey.deleteOnExit()

      override val password: String = encryptedConfig.password
    }
  }
}



