/*
 * Copyright (c) 2026. Dick Lieber, WA9NNN
 *
 * This program is free software: you can redistribute it and/or modify 
 * it under the terms of the GNU General Public License as published by 
 * the Free Software Foundation, either version 3 of the License, or    
 * (at your option) any later version.                                  
 *                                                                      
 * This program is distributed in the hope that it will be useful,      
 * but WITHOUT ANY WARRANTY; without even the implied warranty of       
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the        
 * GNU General Public License for more details.                         
 *                                                                      
 * You should have received a copy of the GNU General Public License    
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package fdswarm.model

import fdswarm.model.BandMode.*
import io.circe.{Codec, Decoder, Encoder}
import sttp.tapir.Schema

/**
 * Allows storng band and mode in a compact why in a [[Qso]]
 */
case class BandMode private[fdswarm] (band: Band, mode: Mode):
  def cabMode: Mode =
    mode match
      case "USB" => "PH"
      case "LSB" => "PH"
      case "SSB" => "PH"
      case "AM" => "PH"
      case "CW" => "CW"
      case _ => "DI"

  override def toString: String =
    s"${band.name} $mode"


object BandMode:
  enum Band(val name: String):
    case Band_160m extends Band("160m")
    case Band_80m extends Band("80m")
    case Band_60m extends Band("60m")
    case Band_40m extends Band("40m")
    case Band_30m extends Band("30m")
    case Band_20m extends Band("20m")
    case Band_15m extends Band("15m")
    case Band_10m extends Band("10m")
    case Band_6m extends Band("6m")
    case Band_4m extends Band("4m")
    case Band_2m extends Band("2m")
    case Band_1_25m extends Band("1.25m")
    case Band_70cm extends Band("70cm")
    case Band_33cm extends Band("33cm")
    case Band_23cm extends Band("23cm")
    case Band_13cm extends Band("13cm")
    case Band_9cm extends Band("9cm")
    case Band_6cm extends Band("6cm")
    case Band_3cm extends Band("3cm")

    override def toString: String = name

  object Band:
    private val byName: Map[String, Band] =
      Band.values.map(b => b.name.toLowerCase -> b).toMap

    def fromString(band: String): Band =
      val normalized = band.trim.toLowerCase
      byName.getOrElse(normalized, throw new IllegalArgumentException(s"Unknown band: '$band'"))

    def fromStringOption(band: String): Option[Band] =
      byName.get(band.trim.toLowerCase)
  type Mode = String

  /**
   * Use when we don't have an explicit frequency
   */
  val bandFreqMap: Map[Band, String] = {
    Seq(
      Band.Band_160m -> "1810",
      Band.Band_80m -> "3530",
      Band.Band_40m -> "7030",
      Band.Band_20m -> "14035",
      Band.Band_15m -> "21030",
      Band.Band_10m -> "28030",
      Band.Band_6m -> "28030",
      Band.Band_2m -> "144000",
      Band.Band_1_25m -> "224000",
      Band.Band_70cm -> "442000",
    ).toMap
  }
  private val Parse = """\s*([\d.]+[a-zA-Z]+)\s+([a-zA-Z]{1,5})\s*""".r

  def apply(band: String, mode: String): BandMode =
    new BandMode(Band.fromString(band), mode.trim.toUpperCase)

  def apply(s: String): BandMode =
    s match
      case Parse(band, mode) =>
        new BandMode(Band.fromString(band), mode.trim.toUpperCase)
      case _ => throw new IllegalArgumentException(s"Can't parse $s")

  def bandToFreq(band: Band): String =
    bandFreqMap.getOrElse(band, "")

  def bandToFreq(band: String): String =
    Band.fromStringOption(band).flatMap(bandFreqMap.get).getOrElse("")

  given Schema[BandMode] = Schema.string
  given Schema[Band] = Schema.string
  given Encoder[Band] = Encoder.encodeString.contramap(_.name)
  given Decoder[Band] = Decoder.decodeString.emap(value =>
    scala.util.Try(Band.fromString(value)).toEither.left.map(_.getMessage)
  )
  given Encoder[BandMode] = Encoder.encodeString.contramap(_.toString)
  given Decoder[BandMode] = Decoder.decodeString.map(BandMode.apply)
