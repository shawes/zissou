package analysis

import java.nio.file.{Files, Path}
import io.circe.{ACursor, Json}

/** One run of a design: its name, and the settings it changes from the baseline, as paths into the configuration. */
case class PlannedRun(name: String, settings: Seq[(String, Json)], seed: Option[Int], label: String)

/**
  * What a sensitivity analysis runs: a baseline configuration, and the runs that each change some of its settings.
  *
  * {{{
  * baseline: data/experiments/configs/nsw-vic-12d-5pc.yaml
  * output: data/sensitivity/stage1        # a folder for each run, and summary.csv
  * parallel: 2                            # runs at once, each in a process of its own
  * flow: data/flow/bran2020-nsw           # optional: read the flow from here instead of where the baseline says
  * seeds: [1, 2, 3]                       # optional: every run once with each of these random seeds
  * regions:                               # optional: the share of settlers on the reefs numbered in each range
  *   victoria: [1000000, 1999999]
  * runs:                                  # optional: runs named here, with what they change
  *   - name: baseline
  *   - name: larvae-300
  *     set:
  *       spawn.spawningLocation[*].numberOfLarvae: 300
  * oneAtATime:                            # optional: a run for each value of each setting, the rest as the baseline
  *   larva.pelagicLarvalDuration.nonSettlementPeriod: [5, 15]
  * }}}
  *
  * A path is the names of the settings from the top of the configuration, joined by dots; a list is followed by `[n]`
  * for its nth item, from 0, or `[*]` for every item.
  */
case class Design(
    baseline: Path,
    output: Path,
    parallel: Int,
    flow: Option[String],
    seeds: Seq[Int],
    regions: Seq[(String, Long, Long)],
    runs: Seq[PlannedRun]
)

object Design {

  private val Name = "^[A-Za-z0-9][A-Za-z0-9._-]*$".r

  /** Reads a design file; paths in it are from the folder it is run in. */
  def read(file: Path): Either[String, Design] =
    io.circe.yaml.parser.parse(Files.readString(file)).left.map(e => s"The design is not valid YAML: ${e.getMessage}").flatMap(parse)

  def parse(json: Json): Either[String, Design] = {
    val top = json.hcursor
    for {
      baseline <- top.get[String]("baseline").left.map(_ => "The design needs a 'baseline' configuration file.")
      output <- top.get[String]("output").left.map(_ => "The design needs an 'output' folder.")
      parallel <- top.getOrElse[Int]("parallel")(1).left.map(_ => "'parallel' must be a whole number.")
      _ <- Either.cond(parallel >= 1, (), "'parallel' must be at least 1.")
      flow <- top.get[Option[String]]("flow").left.map(_ => "'flow' must be a folder or the address of a server.")
      seeds <- top.getOrElse[List[Int]]("seeds")(Nil).left.map(_ => "'seeds' must be a list of whole numbers.")
      _ <- Either.cond(seeds.distinct.size == seeds.size, (), "The seeds must be different from one another.")
      regions <- regionsOf(top.downField("regions"))
      named <- namedRuns(top.downField("runs"))
      varied <- oneAtATime(top.downField("oneAtATime"))
      runs = withSeeds(named ++ varied, seeds)
      _ <- Either.cond(runs.nonEmpty, (), "The design has no runs: give 'runs' or 'oneAtATime'.")
      _ <- runs.groupBy(_.name).collectFirst { case (n, rs) if rs.size > 1 => n }.toLeft(()).left.map(n => s"Two runs are named $n.")
    } yield Design(Path.of(baseline), Path.of(output), parallel, flow, seeds, regions, runs)
  }

  private def regionsOf(cursor: ACursor): Either[String, Seq[(String, Long, Long)]] =
    if (cursor.focus.forall(_.isNull)) Right(Nil)
    else cursor.as[Map[String, List[Long]]].left.map(_ => "Each region must be a pair of reef numbers: [first, last].").flatMap { map =>
      map.toSeq.sortBy(_._1).foldLeft[Either[String, Seq[(String, Long, Long)]]](Right(Nil)) {
        case (Right(acc), (name, List(first, last))) if first <= last => Right(acc :+ (name, first, last))
        case (Right(_), (name, _)) => Left(s"The region $name must be a pair of reef numbers, the first no more than the last.")
        case (left, _) => left
      }
    }

  private def namedRuns(cursor: ACursor): Either[String, Seq[PlannedRun]] =
    if (cursor.focus.forall(_.isNull)) Right(Nil)
    else cursor.values.toRight("'runs' must be a list.").flatMap { items =>
      items.toSeq.foldLeft[Either[String, Seq[PlannedRun]]](Right(Nil)) { (acc, item) =>
        for {
          done <- acc
          name <- item.hcursor.get[String]("name").left.map(_ => "Every run needs a 'name'.")
          _ <- Either.cond(Name.matches(name), (), s"The run name '$name' may only have letters, digits, '.', '_' and '-'.")
          settings <- item.hcursor.downField("set").focus.filterNot(_.isNull) match {
            case None => Right(Nil)
            case Some(set) => set.asObject.map(_.toList).toRight(s"'set' of the run $name must be settings and their values.")
          }
          _ <- settings.map(s => Paths.parse(s._1)).collectFirst { case Left(e) => e }.toLeft(())
        } yield done :+ PlannedRun(name, settings, None, name)
      }
    }

  private def oneAtATime(cursor: ACursor): Either[String, Seq[PlannedRun]] =
    if (cursor.focus.forall(_.isNull)) Right(Nil)
    else cursor.focus.flatMap(_.asObject).toRight("'oneAtATime' must be settings, each with a list of values.").flatMap { factors =>
      factors.toList.foldLeft[Either[String, Seq[PlannedRun]]](Right(Nil)) { case (acc, (path, values)) =>
        for {
          done <- acc
          segments <- Paths.parse(path)
          list <- values.asArray.filter(_.nonEmpty).toRight(s"The values of $path must be a list of one or more.")
        } yield done ++ list.map { value =>
          val shown = value.asString.getOrElse(value.noSpaces)
          val name = s"${segments.last.name}-${shown.replaceAll("[^A-Za-z0-9.]+", "_")}"
          PlannedRun(name, Seq(path -> value), None, s"$path = $shown")
        }
      }
    }

  // every run with each seed; a run that sets the seed itself keeps it
  private def withSeeds(runs: Seq[PlannedRun], seeds: Seq[Int]): Seq[PlannedRun] =
    if (seeds.isEmpty) runs
    else for (run <- runs; seed <- seeds) yield
      run.copy(name = s"${run.name}-s$seed", settings = ("settings.randomSeed" -> Json.fromInt(seed)) +: run.settings, seed = Some(seed))
}

/** Paths into a configuration, such as `spawn.spawningLocation[*].numberOfLarvae`, and setting the value at one. */
object Paths {

  /** One step of a path: a setting, and if it is a list, which of its items (None for all of them). */
  case class Segment(name: String, item: Option[Option[Int]])

  private val Step = """^([A-Za-z_][A-Za-z0-9_]*)(?:\[(\*|\d+)\])?$""".r

  def parse(path: String): Either[String, List[Segment]] = {
    val steps = path.split('.').toList
    val parsed = steps.map {
      case Step(name, null) => Right(Segment(name, None))
      case Step(name, "*") => Right(Segment(name, Some(None)))
      case Step(name, n) => Right(Segment(name, Some(Some(n.toInt))))
      case other => Left(s"'$path' is not a path to a setting: '$other' should be a name, followed by [n] or [*] for a list.")
    }
    parsed.collectFirst { case Left(e) => e }.toLeft(parsed.collect { case Right(s) => s })
  }

  /**
    * The configuration with a value put at a path. The settings along the way must be there, but the last can be new,
    * so that a setting the baseline leaves out can be given.
    */
  def set(config: Json, path: String, value: Json): Either[String, Json] =
    parse(path).flatMap(segments => setAt(config, segments, value, path))

  private def setAt(json: Json, segments: List[Segment], value: Json, path: String): Either[String, Json] = segments match {
    case Nil => Right(value)
    case Segment(name, item) :: rest =>
      json.asObject.toRight(s"$path: there is no setting $name, as what holds it is not a group of settings").flatMap { obj =>
        val existing = obj(name)
        (item, existing) match {
          case (None, _) if existing.isEmpty && rest.nonEmpty => Left(s"$path: the configuration has no $name")
          case (None, _) => setAt(existing.getOrElse(Json.Null), rest, value, path).map(v => Json.fromJsonObject(obj.add(name, v)))
          case (Some(which), Some(list)) if list.isArray =>
            val items = list.asArray.get
            which match {
              case Some(n) if n >= items.size => Left(s"$path: $name has only ${items.size} items")
              case _ =>
                val updated = items.zipWithIndex.map { case (it, i) =>
                  if (which.forall(_ == i)) setAt(it, rest, value, path) else Right(it)
                }
                updated.collectFirst { case Left(e) => e }.toLeft(updated.collect { case Right(j) => j })
                  .map(js => Json.fromJsonObject(obj.add(name, Json.fromValues(js))))
            }
          case _ => Left(s"$path: $name is not a list")
        }
      }
  }
}
