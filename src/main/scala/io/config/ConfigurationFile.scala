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
    val config = scala.io.Source.fromFile(file).mkString
    val json = parser.parse(config)
    json
      .leftMap(err => err: Error)
      .flatMap(_.as[Configuration])
      .valueOr(throw _)
  }

  def write() = {
    // Nothing yet
  }

}
