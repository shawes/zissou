package main

import org.scalatest.FlatSpec

class LarvaeDisperserTest extends FlatSpec {
  "A larval disperser" should "require a non-null configuration file" in {
    intercept[IllegalArgumentException] {
      new LarvaeDisperser(null)
    }
  }
}
