package biology.fish

import grizzled.slf4j.Logging
import locals.OntogenyState.OntogenyState
import locals.SwimmingAbility.SwimmingAbility
import locals.PelagicLarvaeState.PelagicLarvaeState
import locals.DielVerticalMigrationType.DielVerticalMigrationType
import locals.HabitatType
import locals.{Constants, OntogenyState, PelagicLarvaeState, SwimmingAbility, HabitatType}
import org.joda.time.DateTime
import physical.GeoCoordinate
import physical.Velocity
import physical.habitat.HabitatPolygon
import physical.habitat.GeometryAdaptor
import com.github.nscala_time.time.Imports._
import maths.RandomNumberGenerator
import biology._

import scala.collection.mutable.ListBuffer

class Fish(
  val id: Int,
  val pelagicLarvalDuration: Int,
  val maximumLifeSpan: Int,
  val birthplace: Birthplace,
  val spawned: DateTime,
  val fishOntogeny: FishOntogeny,
  val swimming: Swimming,
  val verticalMigrationOntogenetic: FishVerticalMigrationOntogenetic,
  val verticalMigrationDiel: VerticalMigrationDiel)
  extends Larva with Logging {

  val fishHistory = ListBuffer.empty[TimeCapsule]
  var fishState = PelagicLarvaeState.Pelagic
  var fishAge = 0
  var fishSettlementDate: Option[DateTime] = None
  var fishPosition = birthplace.location
  var fishPolygon: Option[Int] = None //TODO: Think about how this works
  //val pelagicHabitat = Some(new GeometryAdaptor(null, -1, HabitatType.Ocean))
  var lastDielMigration : Option[DielVerticalMigrationType] = None
  private var hasChangedOntogeneticState : Boolean = false
  
  var fishDirection : Double = -1

  def this() = this(0, 0, 0, null, DateTime.now(), null, null, null, null)

  override def settlementDate: DateTime = fishSettlementDate.get

  override def changedOntogeneticState : Boolean = hasChangedOntogeneticState
  
  override def direction : Double = fishDirection
  
  override def changeDirection(angle : Double) = fishDirection = angle

  def inCompetencyWindow: Boolean = age < pelagicLarvalDuration && getOntogeny == OntogenyState.Postflexion //TODO: Need to code in a better competency window

  def getOntogeny: OntogenyState = ontogeny.getState(age)

  override def ontogeny: FishOntogeny = fishOntogeny

  override def age: Int = fishAge

  def undergoesOntogeneticMigration : Boolean =  {
    debug("Checking OVM, size is "+ verticalMigrationOntogenetic.probabilities.size)
    verticalMigrationOntogenetic.probabilities.nonEmpty
  }

  def undergoesDielMigration : Boolean = verticalMigrationDiel.probabilities.nonEmpty

  def move(location: GeoCoordinate): Unit = {
    if(location != fishPosition) {
      changeState(PelagicLarvaeState.Pelagic)
      updatePosition(location)
    }
  }

  def updatePosition(newPos: GeoCoordinate): Unit = fishPosition = newPos

  //def horizontalSwimmingSpeed: Double = 0.0 //TODO: Implement the swimming speed

  def growOlder(seconds: Int): Unit = {
    val initialOntogeny = getOntogeny
    fishAge += seconds
    val currentOntogeny = getOntogeny
    if(initialOntogeny == currentOntogeny) {
      hasChangedOntogeneticState = false
    } else {
      hasChangedOntogeneticState = true
    }
  }

  def settle(reefId : Int, date: DateTime): Unit = {
    updateHabitat(reefId)
    fishSettlementDate = Some(date)
    changeState(PelagicLarvaeState.Settled)
  }

  def updateHabitat(reefId: Int): Unit = fishPolygon = Some(reefId)

  def kill(): Unit = {
    changeState(PelagicLarvaeState.Dead)
  }

  private def changeState(newState: PelagicLarvaeState): Unit = {
    fishState = newState
    saveState()
  }

  private def saveState() : Unit = 
    fishHistory append new TimeCapsule(age, getOntogeny, state, polygon, position)

  override def position: GeoCoordinate = fishPosition

  override def polygon: Option[Int] = fishPolygon

  override def ontogeneticVerticallyMigrate: Unit = {
    val depth = verticalMigrationOntogenetic.getDepth(getOntogeny)
    updatePosition(new GeoCoordinate(position.latitude, position.longitude, depth))
  }

  override def dielVerticallyMigrate(dielMigration : DielVerticalMigrationType) : Unit = {
    if(!lastDielMigration.isDefined || lastDielMigration.get != dielMigration) {
      val depth = verticalMigrationDiel.getDepth(dielMigration)
      val newDepth = new GeoCoordinate(position.latitude, position.longitude, depth)
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

  override def history: ListBuffer[TimeCapsule] = fishHistory

  override def state: PelagicLarvaeState = fishState

  override def birthday: DateTime = spawned




}
