package io

import java.io.File
import javax.xml.bind.{JAXBContext, JAXBException, UnmarshalException}

import io.config.Configuration

/**
  * Created by steve on 26/01/2016.
  */
class ConfigurationFileReader extends FileReaderTrait {

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
