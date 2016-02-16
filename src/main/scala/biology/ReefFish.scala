package biology

import grizzled.slf4j.Logging
import locals.HabitatType
import org.joda.time.DateTime
import physical.habitat.{GeometryAdaptor, HabitatPolygon}

class ReefFish(id: Int,
               pelagicLarvalDuration: Int,
               maximumLifeSpan: Int,
               birthplace: Birthplace,
               spawned: DateTime,
               ontogeny: Ontogeny,
               verticalMigration: VerticalMigration)
  extends Larva(
    id,
    pelagicLarvalDuration,
    maximumLifeSpan,
    birthplace,
    spawned,
    ontogeny,
    verticalMigration) with Logging {

  var polygon: HabitatPolygon = new GeometryAdaptor(null, -1, HabitatType.Ocean) //TODO: Think about how this works

  def this() = this(0, 0, 0, null, DateTime.now(), null, null)
}
