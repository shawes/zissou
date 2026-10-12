package maths

/**
  * A cheap approximation of sunrise and sunset, from the NOAA solar equations.
  *
  * It is only good to a few minutes, which is enough to tell that a time is
  * hours away from dawn or dusk. The exact calculator should decide anything
  * closer than that.
  */
object SolarEvents {

  private val SecondsInDay = 86400.0
  // Zenith of the sun's centre at sunrise and sunset, allowing for refraction
  private val OfficialZenith = 90.833

  /**
    * Sunrise and sunset as local wall-clock seconds into the day, or None
    * where the sun does not rise or set that day.
    *
    * @param longitude degrees east
    * @param offsetHours the whole-hour offset of the local time from UTC
    */
  def approximate(
      latitude: Double,
      longitude: Double,
      dayOfYear: Int,
      offsetHours: Int
  ): Option[(Double, Double)] = {
    val g = 2 * math.Pi / 365 * (dayOfYear - 1)
    val equationOfTime = 229.18 * (0.000075 + 0.001868 * math.cos(g) -
      0.032077 * math.sin(g) - 0.014615 * math.cos(2 * g) -
      0.040849 * math.sin(2 * g))
    val declination = 0.006918 - 0.399912 * math.cos(g) +
      0.070257 * math.sin(g) - 0.006758 * math.cos(2 * g) +
      0.000907 * math.sin(2 * g) - 0.002697 * math.cos(3 * g) +
      0.00148 * math.sin(3 * g)
    val lat = math.toRadians(latitude)
    val cosHourAngle =
      math.cos(math.toRadians(OfficialZenith)) / (math.cos(lat) * math.cos(declination)) -
        math.tan(lat) * math.tan(declination)
    if (cosHourAngle.isNaN || cosHourAngle < -1 || cosHourAngle > 1) {
      None
    } else {
      val hourAngle = math.toDegrees(math.acos(cosHourAngle))
      val sunriseUtc = 720 - 4 * (longitude + hourAngle) - equationOfTime
      val sunsetUtc = 720 - 4 * (longitude - hourAngle) - equationOfTime
      Some(((sunriseUtc + 60 * offsetHours) * 60, (sunsetUtc + 60 * offsetHours) * 60))
    }
  }

  /** The distance in seconds between two times of day, going the short way round. */
  def distanceAroundTheClock(a: Double, b: Double): Double = {
    val difference = math.abs(a - b) % SecondsInDay
    math.min(difference, SecondsInDay - difference)
  }
}
