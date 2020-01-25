import sbt._
import sbt.Keys._
import scala.sys.process._

name := "dw-sftp-to-s3"

version := "git describe --tags --dirty --always".!!.stripPrefix("v").trim

scalaVersion := "2.12.4"

libraryDependencies ++= Dependencies.loggingDependencies
libraryDependencies ++= Dependencies.awsDependencies
libraryDependencies ++= Dependencies.decryptionDependencies
libraryDependencies ++= Dependencies.otherDependencies
libraryDependencies ++= Dependencies.sftpDependencies

resolvers += Resolver.jcenterRepo

enablePlugins(JavaAppPackaging)

maintainer in Docker := "Data <data-team@gilt.com>"
dockerBaseImage := "java:8-jre"
defaultLinuxInstallLocation in Docker := "/opt/gilt/dw-sftp-to-s3"
dockerExposedVolumes := Seq("/opt/gilt/dw-sftp-to-s3/logs")
dockerRepository := Some("326027360148.dkr.ecr.us-east-1.amazonaws.com")
dockerEntrypoint := Seq("/opt/gilt/dw-sftp-to-s3/bin/dw-sftp-to-s3")

