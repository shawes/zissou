package model

import org.scalatest.flatspec.AnyFlatSpec

class BatchTest extends AnyFlatSpec {
  "Sharing the cores" should "give each run its share, and at least one core" in {
    assert(Batch.coresPerRun(1, 10) == 10)
    assert(Batch.coresPerRun(2, 10) == 5)
    assert(Batch.coresPerRun(3, 10) == 3)
    assert(Batch.coresPerRun(16, 10) == 1)
  }
}
