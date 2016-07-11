package io.config

import java.io.File
import javax.xml.bind.{JAXBContext, JAXBException, UnmarshalException}

/**
  * Created by steve on 26/01/2016.
  */
class ConfigurationFile {

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

  def write(config: Configuration, file: File) {
    try {
      val context = JAXBContext.newInstance(classOf[Configuration])
      context.createMarshaller.marshal(config, file)
    } catch {
      case ex: JAXBException => println("Marshalling configuration failed" + ex.printStackTrace())
    }
  }

}
