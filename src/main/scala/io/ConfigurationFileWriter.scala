package io

import java.io.File
import javax.xml.bind.{JAXBContext, JAXBException}

import io.config.Configuration

class ConfigurationFileWriter(config: Configuration, file: File) {

  def write() {
    try {
      val context = JAXBContext.newInstance(classOf[Configuration])
      context.createMarshaller.marshal(config, file)
    } catch {
      case ex: JAXBException => println("Marshalling configuration failed" + ex.printStackTrace())
    }
  }
}
