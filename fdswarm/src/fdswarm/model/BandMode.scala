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

import upickle.ReadWriter
import io.circe.{Decoder, Encoder, Codec}
import BandMode.*

/**
 * Allows storng band and mode in a compact why in a [[Qso]]
 */
case class BandMode private[fdswarm] (band: Band, mode: Mode) derives ReadWriter, Codec.AsObject:
  def cabMode: Band =
    mode match
      case "USB" => "PH"
      case "LSB" => "PH"
      case "SSB" => "PH"
      case "AM" => "PH"
      case "CW" => "CW"
      case _ => "DI"

  override def toString: String =
    s"$band $mode"


object BandMode:
  type Band = String
  type Mode = String

  //    implicit val rw: ReadWriter[BandMode] = upickle.readwriter[String].bimap[BandMode](
//      `x => s"${x.i} ${x.s}",
//      str => {
//        val Array(i, s) = str.split(" ", 2)
//        new BandMode(i.toInt, s)
//      }
//    )
  /**
   * Use when we don't have an explicit frequency
   */
  val bandFreqMap: Map[Band, Band] = {
    Seq(
      "160M" -> "1810",
      "80M" -> "3530",
      "40M" -> "7030",
      "20M" -> "14035",
      "15M" -> "21030",
      "10M" -> "28030",
      "6M" -> "28030",
      "2M" -> "144000",
      "1.25M" -> "224000",
      "70cm" -> "442000",
    ).toMap
  }
  private val Parse = """\s*([\d.]+[a-z]+)\s+([A-Z]{2})\s*""".r


  private[fdswarm] def apply(s: String): BandMode =
    s match
      case Parse(band, mode) =>
        new BandMode(band, mode)
      case _ => throw new IllegalArgumentException(s"Can't parse $s")

  def bandToFreq(band: String): String =
    bandFreqMap.getOrElse(band.toUpperCase(), "")
