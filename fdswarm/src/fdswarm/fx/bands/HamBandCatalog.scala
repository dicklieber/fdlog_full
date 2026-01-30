package fdswarm.fx.bands

import com.typesafe.config.{Config, ConfigException}
import jakarta.inject.{Inject, Singleton}

import scala.jdk.CollectionConverters.*

@Singleton
final class HamBandCatalog @Inject()(config: Config):

  private val key = "fdswarm.hamBands"

  lazy val all: List[HamBand] =
    if !config.hasPath(key) then Nil
    else config.getConfigList(key).asScala.toList.map(readBand)

  lazy val byName: Map[String, HamBand] =
    all.map(b => b.bandName.trim.toLowerCase -> b).toMap

  def get(name: String): Option[HamBand] =
    byName.get(name.trim.toLowerCase)

  // ---------------- parsing ----------------

  private def readBand(c: Config): HamBand =
    val bandName = c.getString("bandName")
    val startHz  = c.getLong("startFrequencyHz")
    val endHz    = c.getLong("endFrequencyHz")
    val cls      = BandClass.valueOf(c.getString("bandClass"))

    val ituAvail =
      if c.hasPath("ituRegionAvailability") then
        readItuAvailability(c.getConfig("ituRegionAvailability"))
      else
        // default if omitted
        ItuRegionAvailability.AllRegions

    HamBand(
      bandName = bandName,
      startFrequencyHz = startHz,
      endFrequencyHz = endHz,
      bandClass = cls,
      ituRegionAvailability = ituAvail
    )

  private def readItuAvailability(c: Config): ItuRegionAvailability =
    val t = c.getString("type")

    t match
      case "AllRegions" =>
        ItuRegionAvailability.AllRegions

      case "VariesByCountry" =>
        ItuRegionAvailability.VariesByCountry

      case "RegionsOnly" =>
        val rs =
          if c.hasPath("regions") then
            c.getStringList("regions").asScala.toSet.map(ItuRegion.valueOf)
          else
            throw new ConfigException.Missing("ituRegionAvailability.regions (required for RegionsOnly)")
        ItuRegionAvailability.RegionsOnly(rs)

      case other =>
        throw new ConfigException.BadValue("ituRegionAvailability.type", s"Unknown type: $other")