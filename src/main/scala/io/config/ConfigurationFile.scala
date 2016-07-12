package io.config

import java.io.File
import javax.xml.bind.{JAXBContext, JAXBException, UnmarshalException}

import grizzled.slf4j.Logging

/**
  * Created by steve on 26/01/2016.
  */
class ConfigurationFile extends Logging {

  def read(file: File): Configuration = {
    try {
      val context = JAXBContext.newInstance(classOf[Configuration])
      context.createUnmarshaller().unmarshal(file).asInstanceOf[Configuration]
    } catch {
      case ex: UnmarshalException => error("Un-marshalling configuration failed" + ex.printStackTrace())
        new Configuration()
      case ex: JAXBException => error("Un-marshalling configuration failed" + ex.printStackTrace())
        new Configuration()
    }
  }

  def write(config: Configuration, file: File) {
    try {
      val context = JAXBContext.newInstance(classOf[Configuration])
      context.createMarshaller.marshal(config, file)
    } catch {
      case ex: JAXBException => error("Marshalling configuration failed" + ex.printStackTrace())
    }
  }

}
