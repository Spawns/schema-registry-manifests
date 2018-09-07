package com.sumup

import com.typesafe.config.{Config, ConfigFactory}

trait Configuration {
  implicit val config: Config = ConfigFactory.load()
}
