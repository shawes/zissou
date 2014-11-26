package io

import javax.xml.bind.{UnmarshalException, JAXBException, JAXBContext}
import java.io.File
import io.config.Configuration

class ConfigurationFileWriter {

  def write(config: Configuration, file: File) {
    try {
      val context = JAXBContext.newInstance(classOf[Configuration])
      context.createMarshaller.marshal(config, file)

    } catch {
      case ex: JAXBException => println("Marshalling configuration failed" + ex.printStackTrace())
    }
  }

  def read(file: File): Configuration = {
    try {
      val context = JAXBContext.newInstance(classOf[Configuration])
      context.createUnmarshaller().unmarshal(file).asInstanceOf[Configuration]

    } catch {
      case ex: UnmarshalException => println("Un-marshalling configuration failed" + ex.printStackTrace())
        new Configuration()
      case ex: JAXBException => println("Un-marshalling configuration failed" + ex.printStackTrace())
        new Configuration()
    }
  }

}
