package io.config

import java.io.File
import java.io.InputStreamReader
import io.circe.yaml.parser
import cats.syntax.either._
import io.circe._
import io.circe.generic.auto._
import io.circe.yaml
import grizzled.slf4j._

class ConfigurationFile extends Logging {

  def read(file: String): Configuration = {
    val config = getClass.getClassLoader.getResourceAsStream(file)
    val json = parser.parse(new InputStreamReader(config))
    json
      .leftMap(err => err: Error)
      .flatMap(_.as[Configuration])
      .valueOr(throw _)
  }

  def write() = {
    // Nothing yet
  }

}
