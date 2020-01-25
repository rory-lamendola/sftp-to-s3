package Configs

import com.typesafe.config.Config

case class Configs(
  job: String,
  sftp: SftpConfig,
  encryption: EncryptionConfig,
  aws: AWSConfig
)


object Configs {
  def apply(config: Config, job: String): Configs = {
    Configs(
      job,
      SftpConfig(config.getConfig(s"$job.sftp"), job),
      EncryptionConfig(config.getConfig(s"$job.encryption"), job),
      AWSConfig(config.getConfig(s"$job.aws"), job)
      )
  }
}