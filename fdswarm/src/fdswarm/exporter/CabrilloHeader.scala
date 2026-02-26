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

package fdswarm.exporter

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.*

enum CategoryOperator(val value: String):
  case SINGLE_OP extends CategoryOperator("SINGLE-OP")
  case MULTI_OP extends CategoryOperator("MULTI-OP")
  case CHECKLOG extends CategoryOperator("CHECKLOG")
  override def toString: String = value

enum CategoryAssisted(val value: String):
  case ASSISTED extends CategoryAssisted("ASSISTED")
  case NON_ASSISTED extends CategoryAssisted("NON-ASSISTED")
  override def toString: String = value

enum CategoryBand(val value: String):
  case ALL extends CategoryBand("ALL")
  case B160 extends CategoryBand("160M")
  case B80 extends CategoryBand("80M")
  case B40 extends CategoryBand("40M")
  case B20 extends CategoryBand("20M")
  case B15 extends CategoryBand("15M")
  case B10 extends CategoryBand("10M")
  case B6 extends CategoryBand("6M")
  case B2 extends CategoryBand("2M")
  case B222 extends CategoryBand("222")
  case B432 extends CategoryBand("432")
  case B902 extends CategoryBand("902")
  case B1_2G extends CategoryBand("1.2G")
  override def toString: String = value

enum CategoryMode(val value: String):
  case CW extends CategoryMode("CW")
  case DIGI extends CategoryMode("DIGI")
  case FM extends CategoryMode("FM")
  case RTTY extends CategoryMode("RTTY")
  case SSB extends CategoryMode("SSB")
  case MIXED extends CategoryMode("MIXED")
  override def toString: String = value

enum CategoryOperatorAge(val value: String):
  case NONE extends CategoryOperatorAge("")
  case YOUTH extends CategoryOperatorAge("YOUTH")
  override def toString: String = value

enum CategoryPower(val value: String):
  case HIGH extends CategoryPower("HIGH")
  case LOW extends CategoryPower("LOW")
  case QRP extends CategoryPower("QRP")
  override def toString: String = value

enum CategoryStation(val value: String):
  case FIXED extends CategoryStation("FIXED")
  case MOBILE extends CategoryStation("MOBILE")
  case PORTABLE extends CategoryStation("PORTABLE")
  case ROVER extends CategoryStation("ROVER")
  case ROVER_LIMITED extends CategoryStation("ROVER-LIMITED")
  case ROVER_UNLIMITED extends CategoryStation("ROVER-UNLIMITED")
  case EXPEDITION extends CategoryStation("EXPEDITION")
  case HQ extends CategoryStation("HQ")
  case SCHOOL extends CategoryStation("SCHOOL")
  override def toString: String = value

enum CategoryTransmitter(val value: String):
  case ONE extends CategoryTransmitter("ONE")
  case TWO extends CategoryTransmitter("TWO")
  case LIMITED extends CategoryTransmitter("LIMITED")
  case UNLIMITED extends CategoryTransmitter("UNLIMITED")
  case SWL extends CategoryTransmitter("SWL")
  override def toString: String = value

enum CategoryOverlay(val value: String):
  case NONE extends CategoryOverlay("")
  case CLASSIC extends CategoryOverlay("CLASSIC")
  case ROOKIE extends CategoryOverlay("ROOKIE")
  case TB_WIRES extends CategoryOverlay("TB-WIRES")
  case NOVICE_TECH extends CategoryOverlay("NOVICE-TECH")
  case OVER_50 extends CategoryOverlay("OVER-50")
  override def toString: String = value

case class CabrilloHeader(
  callsign: String = "",
  contest: String = "",
  categoryOperator: CategoryOperator = CategoryOperator.MULTI_OP,
  categoryAssisted: CategoryAssisted = CategoryAssisted.NON_ASSISTED,
  categoryBand: CategoryBand = CategoryBand.ALL,
  categoryMode: CategoryMode = CategoryMode.MIXED,
  categoryOperatorAge: CategoryOperatorAge = CategoryOperatorAge.NONE,
  categoryPower: CategoryPower = CategoryPower.LOW,
  categoryStation: CategoryStation = CategoryStation.FIXED,
  categoryTransmitter: CategoryTransmitter = CategoryTransmitter.ONE,
  categoryOverlay: CategoryOverlay = CategoryOverlay.NONE,
  claimedScore: Option[Int] = None,
  club: String = "",
  operators: String = "",
  name: String = "",
  address: String = "",
  addressCity: String = "",
  addressStateProvince: String = "",
  addressPostalCode: String = "",
  addressCountry: String = "",
  stationClass: String = "",
  stationSection: String = "",
  soapbox: String = ""
)

object CabrilloHeader:
  import io.circe.generic.semiauto.*
  given Encoder[CabrilloHeader] = deriveEncoder
  given Decoder[CabrilloHeader] = deriveDecoder

  private def enumDecoder[T](values: Array[T], name: T => String): Decoder[T] =
    Decoder.decodeString.emap { s =>
      values.find(v => name(v) == s).toRight(s"Invalid enum value: $s")
    }

  given Decoder[CategoryOperator] = enumDecoder(CategoryOperator.values, _.toString)
  given Decoder[CategoryAssisted] = enumDecoder(CategoryAssisted.values, _.toString)
  given Decoder[CategoryBand] = enumDecoder(CategoryBand.values, _.toString)
  given Decoder[CategoryMode] = enumDecoder(CategoryMode.values, _.toString)
  given Decoder[CategoryOperatorAge] = enumDecoder(CategoryOperatorAge.values, _.toString)
  given Decoder[CategoryPower] = enumDecoder(CategoryPower.values, _.toString)
  given Decoder[CategoryStation] = enumDecoder(CategoryStation.values, _.toString)
  given Decoder[CategoryTransmitter] = enumDecoder(CategoryTransmitter.values, _.toString)
  given Decoder[CategoryOverlay] = enumDecoder(CategoryOverlay.values, _.toString)

  given [T <: reflect.Enum]: Encoder[T] = Encoder.encodeString.contramap(_.toString)
