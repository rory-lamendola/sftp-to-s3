package Configs

import com.typesafe.config.Config

trait EncryptionConfig

object EncryptionConfig {
  def apply(encryptedConfig: Config, job: String): EncryptionConfig = {
    if (encryptedConfig.getBoolean("encrypted")) EncryptedConfig(encryptedConfig, job)
    else DecryptedConfig()

  }
}

case class EncryptedConfig(
  publicKeyFile: String,
  privateKeyFile: String,
  password: String,
  aws: AWSConfig,
  job: String
) extends EncryptionConfig {
}

object EncryptedConfig {
  def apply(encryptedConfig: Config, job: String): EncryptedConfig = {
    EncryptedConfig(encryptedConfig.getString("publicKey")
      , encryptedConfig.getString("privateKey")
      , encryptedConfig.getString("password")
      , AWSConfig(encryptedConfig.getConfig("aws"), job)
      , job
    )
  }
}

case class DecryptedConfig() extends EncryptionConfig



