import sbt._

object Dependencies {
  val decryptionDependencies = Seq(
    "name.neuhalfen.projects.crypto.bouncycastle.openpgp" % "bouncy-gpg" % "2.0.1"
  )

  val loggingDependencies = Seq(
    "org.apache.logging.log4j" %% "log4j-api-scala" % "11.0",
    "org.apache.logging.log4j" % "log4j-api" % "2.8.2",
    "org.apache.logging.log4j" % "log4j-core" % "2.8.2" % Runtime
  )

  val sftpDependencies = Seq(
    "com.jcraft" % "jsch" % "0.1.54"
  )

  val awsDependencies = Seq(
    "com.amazonaws" % "aws-java-sdk-s3" % "1.11.277",
    "com.amazonaws" % "aws-java-sdk-ssm" % "1.11.277"
  )

  val otherDependencies = Seq(
    "com.typesafe" % "config" % "1.3.1",
    "org.scalatest" %% "scalatest" % "3.0.4" % "test",
    "org.mockito" % "mockito-core" % "2.15.0",
    "commons-io" % "commons-io" % "2.6"
  )


}