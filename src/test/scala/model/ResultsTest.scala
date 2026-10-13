package model

import java.io.File
import scala.io.Source
import io.GisShapeFile
import org.scalatest.flatspec.AnyFlatSpec
import tools.ModelFixture

class ResultsTest extends AnyFlatSpec {

  "Larvae still drifting when the run ends" should "be in the dispersal results, to where they are at the end" in {
    val inputs = ModelFixture.inputs()
    // a ten day PLD in a three day run: none settle, die or stop drifting before it ends
    val larva = ModelFixture.driftingLarva(pld = 10, nonSettlementPeriod = 10)
    val model = ModelFixture.run(ModelFixture.yaml(inputs, larva, periodEnd = "2002-01-08", includeLarvaeHistory = true))
    val drifting = model.biology.pelagicLarvae
    assert(drifting.size == 50 && model.biology.stationaryLarvae.isEmpty)

    val output = new File(inputs, "output/fixture")
    // every one of them has a path in the dispersal shapefile (all are sampled at 100%)
    assert(new GisShapeFile().read(new File(output, "fixture-dispersal.shp").getPath).size == 50)
    // and none in what settled
    assert(new GisShapeFile().read(new File(output, "fixture-settled.shp").getPath).isEmpty)
    val matrix = Source.fromFile(new File(output, "fixture-connectivity-matrix.csv"))
    try assert(matrix.getLines().size == 1, "only the header") finally matrix.close()

    // the history of each ends where it is, still pelagic
    val log = Source.fromFile(new File(output, "larvae-log.csv"))
    val rows = try log.getLines().drop(1).map(_.split(",")).toList finally log.close()
    val last = rows.groupBy(_(0)).view.mapValues(_.last).toMap
    assert(last.size == 50)
    for (larva <- drifting) {
      val row = last(larva.id)
      assert(row(6) == "pelagic", s"${larva.id} ends ${row(6)}")
      assert(row(8).toDouble == BigDecimal(larva.position.latitude).setScale(5, BigDecimal.RoundingMode.HALF_UP).toDouble)
      assert(row(9).toDouble == BigDecimal(larva.position.longitude).setScale(5, BigDecimal.RoundingMode.HALF_UP).toDouble)
    }
  }
}
