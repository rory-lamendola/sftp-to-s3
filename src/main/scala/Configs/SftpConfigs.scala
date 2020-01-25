package Configs

import com.typesafe.config.Config


trait SftpConfig {
  val host: String
  val path: String
  val filter: String
}

object SftpConfig {
  def apply(sftpConfig: Config, job: String): SftpConfig = {
    if (sftpConfig.getString("type").equals("password")) PasswordConfig(sftpConfig)
    else KeyConfig(sftpConfig, job)
  }
}

case class PasswordConfig(
  username: String,
  host: String,
  password: String,
  path: String,
  filter: String
) extends SftpConfig

object PasswordConfig {
  def apply(sftpConfig: Config): PasswordConfig = {
    PasswordConfig(
      sftpConfig.getString("username"),
      sftpConfig.getString("host"),
      sftpConfig.getString("password"),
      sftpConfig.getString("path"),
      sftpConfig.getString("filter"))
  }
}


case class KeyConfig(
  username: String,
  host: String,
  keyName: String,
  aws: AWSConfig,
  path: String,
  filter: String
) extends SftpConfig

object KeyConfig {
  def apply(sftpConfig: Config, job: String): KeyConfig = {
    KeyConfig(
      sftpConfig.getString("username"),
      sftpConfig.getString("host"),
      sftpConfig.getString("key.keyName"),
      AWSConfig(sftpConfig.getConfig("key.aws"), job),
      sftpConfig.getString("path"),
      sftpConfig.getString("filter"))
  }
}


