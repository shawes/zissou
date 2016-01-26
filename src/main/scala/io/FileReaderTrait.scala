package io

import java.io.File

/**
  * Created by steve on 26/01/2016.
  */
trait FileReaderTrait {
  def read(file: File): Any
}
