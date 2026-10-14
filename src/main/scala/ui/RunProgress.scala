package ui

import io.circe.Encoder

/** `flowSeconds` of a day's `seconds` were spent waiting for the ocean model's flow data. */
/** The larvae in the water from one release site. */
case class SiteCount(site: String, larvae: Int) derives Encoder.AsObject

case class DayPoint(date: String, larvae: Int, seconds: Int, flowSeconds: Int = 0, sites: Seq[SiteCount] = Seq.empty) derives Encoder.AsObject

/** The run is waiting for the flow data of a day, and has been for this long. */
case class FlowWait(date: String, seconds: Int, source: String) derives Encoder.AsObject

/** What a run's log says about how it is getting on. */
case class LogSummary(
    days: Seq[DayPoint],
    finished: Boolean,
    warnings: Seq[String],
    problems: Seq[String],
    waiting: Option[FlowWait] = None
) derives Encoder.AsObject

object RunProgress {

  private val Day =
    """Day (\d{4}-\d{2}-\d{2}) has been completed in (\d+) secs with (\d+) larvae(?: \(waited (\d+) secs for flow data\))?""".r.unanchored

  private val BySite = """\[by site: ([^\]]*)\]""".r.unanchored

  private def sitesOf(line: String): Seq[SiteCount] = line match {
    case BySite(list) =>
      list.split('|').toSeq.flatMap { entry =>
        val at = entry.lastIndexOf('=')
        if (at < 0) None else entry.substring(at + 1).trim.toIntOption.map(SiteCount(entry.substring(0, at), _))
      }
    case _ => Seq.empty
  }

  private val Waiting =
    """Waiting for the flow data for (\d{4}-\d{2}-\d{2}) from (\S+): (\d+) secs so far""".r.unanchored

  def parse(log: String): LogSummary = {
    val lines = log.linesIterator.toSeq
    val days = lines.collect { case line @ Day(date, seconds, larvae, waited) =>
      DayPoint(date, larvae.toInt, seconds.toInt, Option(waited).map(_.toInt).getOrElse(0), sitesOf(line))
    }
    val finished = lines.exists(_.contains("Simulation run completed"))
    // waiting now: the last thing the log says about the flow is that it is being waited for, and no day has been finished since
    val lastDay = lines.lastIndexWhere(l => Day.findFirstIn(l).isDefined)
    val lastWait = lines.lastIndexWhere(l => Waiting.findFirstIn(l).isDefined)
    val waiting =
      if (finished || lastWait < 0 || lastWait < lastDay) None
      else lines(lastWait) match { case Waiting(date, source, seconds) => Some(FlowWait(date, seconds.toInt, source)); case _ => None }
    LogSummary(
      days,
      finished,
      lines.filter(_.contains(" WARN ")).map(_.replaceFirst("^\\[[^\\]]*\\] WARN \\S+ - ", "")),
      // a failed run still exits normally, so the log is the only place it shows
      lines.filter(l => l.contains("Exception") || l.contains(" ERROR ")).map(_.replaceFirst("^\\[[^\\]]*\\] ERROR \\S+ - ", "")).take(5),
      waiting
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
