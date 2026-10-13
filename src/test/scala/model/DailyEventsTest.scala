package model

import com.github.nscala_time.time.Imports._
import locals.TimeStepType
import org.scalatest.flatspec.AnyFlatSpec
import physical.TimeStep
import tools.ModelFixture

class DailyEventsTest extends AnyFlatSpec {

  private val inputs = ModelFixture.inputs()

  private val stepLengths = Seq(
    "half an hour" -> "unit: Second\n    duration: 1800",
    "2 hours" -> "unit: Hour\n    duration: 2",
    "5 hours" -> "unit: Hour\n    duration: 5",
    "a day" -> "unit: Day\n    duration: 1"
  )

  "The first step of a day" should "come once on every day, whatever the length of the step" in {
    for ((seconds, name) <- Seq(1800 -> "half an hour", 7200 -> "2 hours", 18000 -> "5 hours", 86400 -> "a day")) {
      val start = new DateTime(2002, 1, 5, 0, 0, DateTimeZone.UTC)
      val clock = new SimulationClock(start to start.plusDays(30), new TimeStep(seconds, TimeStepType.Second))
      val firsts = scala.collection.mutable.ArrayBuffer.empty[LocalDate]
      while (clock.stillTime) {
        if (clock.isFirstStepOfDay) firsts += clock.now.toLocalDate
        clock.tick()
      }
      val days = (0 until 30).map(day => new LocalDate(2002, 1, 5).plusDays(day))
      assert(firsts.toSeq == days, s"with steps of $name")
    }
  }

  "Larvae" should "be released once on each release day, whatever the length of the step" in {
    for ((name, timeStep) <- stepLengths) {
      // four release days, the 5th to the 8th
      val model = ModelFixture.run(ModelFixture.yaml(inputs, ModelFixture.driftingLarva(), numberOfLarvae = 10,
        releaseEnd = "2002-01-09", timeStep = timeStep))
      val larvae = model.biology.stationaryLarvae ++ model.biology.pelagicLarvae
      assert(larvae.size == 40, s"with steps of $name")
      assert(larvae.map(_.birthday.toLocalDate).distinct.sorted ==
        (5 to 8).map(day => new LocalDate(2002, 1, day)), s"with steps of $name")
    }
  }

  "Daily mortality" should "be applied once a day, whatever the length of the step" in {
    for ((name, timeStep) <- stepLengths) {
      // half die on each day of a three day PLD: an eighth live to die of old age
      val larva = ModelFixture.driftingLarva(pld = 3, nonSettlementPeriod = 3, mortalityRate = Some(0.5))
      val model = ModelFixture.run(ModelFixture.yaml(inputs, larva, numberOfLarvae = 2000, timeStep = timeStep))
      val larvae = model.biology.stationaryLarvae
      assert(larvae.size == 2000 && larvae.forall(_.isDead), s"with steps of $name")
      val oldAge = larvae.count(larva => larva.age > larva.pelagicLarvalDuration)
      // 250 expected, with a standard deviation of 15
      assert(oldAge > 190 && oldAge < 310, s"$oldAge of 2000 lived out their PLD with steps of $name")
    }
  }
}
