package ui

import io.circe.Encoder

case class DayPoint(date: String, larvae: Int, seconds: Int) derives Encoder.AsObject

/** What a run's log says about how it is getting on. */
case class LogSummary(
    days: Seq[DayPoint],
    finished: Boolean,
    warnings: Seq[String],
    problems: Seq[String]
) derives Encoder.AsObject

object RunProgress {

  private val Day =
    """Day (\d{4}-\d{2}-\d{2}) has been completed in (\d+) secs with (\d+) larvae""".r.unanchored

  def parse(log: String): LogSummary = {
    val lines = log.linesIterator.toSeq
    val days = lines.collect { case Day(date, seconds, larvae) =>
      DayPoint(date, larvae.toInt, seconds.toInt)
    }
    LogSummary(
      days,
      lines.exists(_.contains("Simulation run completed")),
      lines.filter(_.contains(" WARN ")).map(_.replaceFirst("^\\[[^\\]]*\\] WARN \\S+ - ", "")),
      // a failed run still exits normally, so the log is the only place it shows
      lines.filter(l => l.contains("Exception") || l.contains(" ERROR ")).map(_.replaceFirst("^\\[[^\\]]*\\] ERROR \\S+ - ", "")).take(5)
    )
  }

  /** How far through the configured period the last completed day is, from 0 to 1. */
  def fraction(summary: LogSummary, periodStart: String, periodEnd: String): Option[Double] =
    for {
      last <- summary.days.lastOption
      start <- scala.util.Try(java.time.LocalDate.parse(periodStart.take(10))).toOption
      end <- scala.util.Try(java.time.LocalDate.parse(periodEnd.take(10))).toOption
      now <- scala.util.Try(java.time.LocalDate.parse(last.date)).toOption
      total = java.time.temporal.ChronoUnit.DAYS.between(start, end).toDouble
      if total > 0
    } yield math.min(1.0, math.max(0.0, java.time.temporal.ChronoUnit.DAYS.between(start, now) / total))
}
