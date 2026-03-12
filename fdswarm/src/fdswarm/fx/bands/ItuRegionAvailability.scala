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

package fdswarm.fx.bands

import io.circe.{Codec, Decoder, Encoder}
import fdswarm.model.Choice
import fdswarm.model.BandMode.*
enum BandClass:
  case LF, VLF, MF, HF, VHF, UHF, SHF, EHF

object BandClass:
  given Codec[BandClass] = Codec.from(
    Decoder.decodeString.emap(s =>
      try Right(BandClass.valueOf(s))
      catch case _: IllegalArgumentException => Left(s"Invalid BandClass: $s")
    ),
    Encoder.encodeString.contramap(_.toString)
  )

enum ItuRegion:
  case ALL, R1, R2, R3

object ItuRegion:
  given Codec[ItuRegion] = Codec.from(
    Decoder.decodeString.emap(s =>
      try Right(ItuRegion.valueOf(s))
      catch case _: IllegalArgumentException => Left(s"Invalid ItuRegion: $s")
    ),
    Encoder.encodeString.contramap(_.toString)
  )


/**
 * These are loaded from application.conf.
 *
 * @param bandName 30m, 40m, 80m, etc. Shown in UI.
 * @param startFrequencyHz used to map radio frequencies to [[HamBand]]
 * @param endFrequencyHz used to map radio frequencies to [[HamBand]]
 * @param bandClass common user-friendly name for band, e.g. "VHF"
 * @param regions where in the world this band is available.
 */
final case class HamBand(
                          bandName: String,
                          startFrequencyHz: Long,
                          endFrequencyHz: Long,
                          bandClass: BandClass,
                          regions: Set[ItuRegion] = Set(ItuRegion.ALL)
                        ) extends Choice[Band]:
  override val value: Band = bandName
  override val label: String = bandName

object HamBand:
  given Codec[HamBand] = Codec.from(
    Decoder.instance { c =>
      for {
        name <- c.downField("bandName").as[String]
        start <- c.downField("startFrequencyHz").as[Long]
        end <- c.downField("endFrequencyHz").as[Long]
        bc <- c.downField("bandClass").as[BandClass]
        regions <- c.downField("regions").as[Option[Set[ItuRegion]]].map(_.getOrElse(Set(ItuRegion.ALL)))
      } yield HamBand(name, start, end, bc, regions)
    },
    Encoder.instance { h =>
      io.circe.Json.obj(
        "bandName" -> Encoder[String].apply(h.bandName),
        "startFrequencyHz" -> Encoder[Long].apply(h.startFrequencyHz),
        "endFrequencyHz" -> Encoder[Long].apply(h.endFrequencyHz),
        "bandClass" -> Encoder[BandClass].apply(h.bandClass),
        "regions" -> Encoder[Set[ItuRegion]].apply(h.regions)
      )
    }
  )
