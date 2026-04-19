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

package fdswarm.fx.contest

import fdswarm.model.Callsign
import fdswarm.util.HamPhonetic.fromString
import io.circe.Codec

import java.time.{Instant, ZoneId}
import java.time.format.DateTimeFormatter

trait ContestConfigFields:
  def contestType: ContestType
  def ourCallsign: Callsign
  def transmitters: Int
  def ourClass: String
  def ourSection: String

/**
 * @param transmitters number of transmitters
 * @param ourCallsign  our callsign
 * @param ourClass     our class
 * @param ourSection   our section
 * @param contestType WFD or ARRL
 * @param stamp        when the config was created. The latest is considered authorative.
 */
case class ContestConfig(contestType: ContestType,
                         ourCallsign: Callsign,
                         transmitters: Int,
                         ourClass: String,
                         ourSection: String,
                         stamp: Instant = Instant.now()) extends ContestConfigFields derives Codec.AsObject:
  require(ourClass.nonEmpty, "ourClass must not be empty")
  require(ourSection.nonEmpty, "ourSection must not be empty")
  private val stampFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd:HH:mm:ss").withZone(ZoneId.systemDefault())

  val exchange:String=
    s"$transmitters$ourClass $ourSection"
  def weAre(usePhonetic: Boolean): String =
    val callsignValue = Option(ourCallsign).map(_.toString).getOrElse("")
    if usePhonetic then
      s"We are ${fromString(callsignValue)} $transmitters ${fromString(ourClass)} ${fromString(ourSection)}"
    else
      s"We are $callsignValue $transmitters$ourClass $ourSection"

  val display:String=
    s"$exchange ${stampFormatter.format(stamp)}"


object ContestConfig:
  val noContest: ContestConfig = ContestConfig(
    contestType = ContestType.NONE,
    ourCallsign = Callsign(""),
    transmitters = 0,
    ourClass = "-",
    ourSection = "-"
  )
