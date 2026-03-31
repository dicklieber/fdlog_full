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

trait ContestConfigFields:
  def contestType: ContestType
  def ourCallsign: Callsign
  def transmitters: Int
  def ourClass: String
  def ourSection: String

/**
 * What a user can choose in a dialog.
 *
 * @param contestType WFD or ARRL
 */
case class ContestConfig(contestType: ContestType,
                         ourCallsign: Callsign,
                         transmitters: Int,
                         ourClass: String,
                         ourSection: String) extends ContestConfigFields derives Codec.AsObject:
  val exchange:String=
    s"$transmitters$ourClass $ourSection"
  def weAre(usePhonetic: Boolean): String =
    if usePhonetic then
      s"We are ${fromString(ourCallsign.toString)} $transmitters ${fromString(ourClass)} ${fromString(ourSection)}"
    else
      s"We are $ourCallsign $transmitters$ourClass $ourSection"

