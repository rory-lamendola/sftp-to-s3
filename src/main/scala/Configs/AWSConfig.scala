package Configs
import com.typesafe.config.Config

case class AWSConfig(
  name: String,
  kms: String,
  trimExtension: Option[String],
  job: String
)

object AWSConfig {
  def apply(S3Config: Config, job: String): AWSConfig = {
    val trimExtension =
      if (S3Config.hasPath("trimExtension")){
        Some(S3Config.getString("trimExtension"))
      } else Option.empty[String]

    AWSConfig(
      S3Config.getString("bucket"),
      S3Config.getString("kms"),
      trimExtension,
      job)
  }
}


