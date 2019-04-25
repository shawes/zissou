package biology.fish

import grizzled.slf4j.Logging
import locals._
import locals.Constants.LightWeightException._
import physical.GeoCoordinate
import com.github.nscala_time.time.Imports._
import biology._
import biology.swimming._
import scala.collection.mutable.ArrayBuffer
import maths.{Geometry, RandomNumberGenerator}

class Fish(
    val id: String,
    val pelagicLarvalDuration: Int,
    val maximumLifeSpan: Int,
    val birthplace: Birthplace,
    val spawned: LocalDateTime,
    override val hatching: Int,
    override val preflexion: Int,
    override val flexion: Int,
    override val postflexion: Int,
    val verticalMigrationOntogenetic: VerticalMigrationOntogenetic,
    val verticalMigrationDiel: VerticalMigrationDiel,
    val nonSettlementPeriod: Int
) extends Larva
    with Logging
    with Swimming
    with OntogenyFish {

  val fishHistory = ArrayBuffer.empty[TimeCapsule]
  var fishState: PelagicLarvaeState = Pelagic
  var fishAge = 0
  var fishSettlementDate: Option[LocalDateTime] = None
  var fishPosition = birthplace.location
  var fishPolygon: Int = 0
  var lastDielMigration: Option[DielVerticalMigrationType] = None
  var nightDepth: Double = -1
  private var hasChangedOntogeneticState: Boolean = false
  var fishDirection: Double = NoSwimmingAngleException
  val geometry = new Geometry()

  override def settlementDate: LocalDateTime = fishSettlementDate.get

  override def changedOntogeneticState: Boolean = hasChangedOntogeneticState

  override def direction: Double = fishDirection

  override def changeDirection(angle: Double): Unit = {
    if (angle != NoSwimmingAngleException) {
      fishDirection = angle
    } else {
      fishDirection = RandomNumberGenerator.getAngle
    }
  }

  def inOlfactoryCompetencyWindow: Boolean =
    age <= pelagicLarvalDuration &&
      getOntogeny == Postflexion &&
      horizontalSwimming.get.isDirected

  def inSettlementCompetencyWindow: Boolean = age >= nonSettlementPeriod

  //def canSmell : Boolean = swimming.isDirected && inOlfactoryCompetencyWindow
  //
  def canSwim: Boolean =
    getState(age) == Flexion || getState(age) == Postflexion

  def getOntogeny: OntogeneticState = getState(age)

  //override def ontogeny: OntogenyFish = fishOntogeny

  override def age: Int = fishAge

  def undergoesOntogeneticMigration: Boolean = {
    trace(
      "Checking OVM, size is " + verticalMigrationOntogenetic.probabilities.size
    )
    verticalMigrationOntogenetic.probabilities.nonEmpty
  }

  override def ontogeneticVerticallyMigrateType
      : OntogeneticVerticalMigrationImpl =
    verticalMigrationOntogenetic.implementation

  def undergoesDielMigration: Boolean =
    verticalMigrationDiel.probabilities.nonEmpty

  def move(location: GeoCoordinate): Unit = {
    if (location != fishPosition) {
      changeState(Pelagic)
      updatePosition(location)
    }
  }

  def updatePosition(newPos: GeoCoordinate): Unit = {
    fishPosition = newPos
  }

  def growOlder(seconds: Int): Unit = {
    val initialOntogeny = getOntogeny
    fishAge += seconds
    val currentOntogeny = getOntogeny
    if (initialOntogeny == currentOntogeny) {
      hasChangedOntogeneticState = false
    } else {
      hasChangedOntogeneticState = true
    }
  }

  def settle(reefId: Int, date: LocalDateTime): Unit = {
    updateHabitat(reefId)
    fishSettlementDate = Some(date)
    changeState(Settled)
  }

  def updateHabitat(reefId: Int): Unit = {
    fishPolygon = reefId
  }

  def kill(): Unit = {
    changeState(Dead)
  }

  private def changeState(newState: PelagicLarvaeState): Unit = {
    fishState = newState
    saveState()
  }

  private def saveState(): Unit =
    fishHistory append new TimeCapsule(
      age,
      getOntogeny,
      state,
      polygon,
      position
    )

  override def position: GeoCoordinate = fishPosition

  override def polygon: Int = fishPolygon

  override def ontogeneticVerticallyMigrate: Unit = {

    val depth =
      verticalMigrationOntogenetic.getDepth(getOntogeny, position.depth)
    updatePosition(
      new GeoCoordinate(position.latitude, position.longitude, depth)
    )
  }

  override def dielVerticallyMigrate(
      dielMigration: DielVerticalMigrationType
  ): Unit = {
    if (!lastDielMigration.isDefined || lastDielMigration.get != dielMigration) {
      var newDepth = position
      if (undergoesOntogeneticMigration && dielMigration == Day) {
        val depth = verticalMigrationDiel.getDepth(dielMigration)
        nightDepth = position.depth
        newDepth =
          new GeoCoordinate(position.latitude, position.longitude, depth)
      } else if (undergoesOntogeneticMigration && dielMigration == Night) {
        if (nightDepth >= 0)
          newDepth =
            new GeoCoordinate(position.latitude, position.longitude, nightDepth)
      } else {
        val depth = verticalMigrationDiel.getDepth(dielMigration)
        newDepth =
          new GeoCoordinate(position.latitude, position.longitude, depth)
      }
      updatePosition(newDepth)
      lastDielMigration = Some(dielMigration)
    }
  }

  override def toString: String =
    "id:" + id + "," +
      "birthday:" + birthday + "," +
      "age:" + age / Constants.SecondsInDay + "," +
      "pld:" + pelagicLarvalDuration / Constants.SecondsInDay + "," +
      "birthplace:" + birthplace.name + "," +
      "state:" + state + "," +
      "history:" + history.size

  override def history: ArrayBuffer[TimeCapsule] = fishHistory

  override def state: PelagicLarvaeState = fishState

  override def birthday: LocalDateTime = spawned

}
