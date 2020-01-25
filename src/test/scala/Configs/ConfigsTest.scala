package Configs

import com.typesafe.config.ConfigFactory
import org.scalatest.FlatSpec

class ConfigsTest extends FlatSpec {

  it should "successfully parse the example config" in {

    val config: Configs = Configs(ConfigFactory.load("application.example.conf"), "JobName2")
    assert(config.job == "JobName2")
    assert(config.aws.trimExtension === Option.empty[String])

  }
  it should "parse the trim file extension if present" in {
    val config: Configs = Configs(ConfigFactory.load("application.example.conf"), "JobName3")
    assert(config.job == "JobName3")
    assert(config.aws.trimExtension.contains(".ppg"))

  }
}
