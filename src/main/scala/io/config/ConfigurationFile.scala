package io.config

import io.circe.yaml.parser
import cats.syntax.either._
import io.circe._
import grizzled.slf4j._

class ConfigurationFile extends Logging {

  def read(file: String): Configuration = {
    val source = scala.io.Source.fromFile(file)
    try parse(source.mkString).valueOr(throw _)
    finally source.close()
  }

  /** Reads a configuration from YAML text, without throwing. */
  def parse(text: String): Either[Error, Configuration] =
    parser
      .parse(text)
      .leftMap(err => err: Error)
      .flatMap(_.as[Configuration])

  def write() = {
    // Nothing yet
  }

}
