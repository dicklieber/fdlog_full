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

import fdswarm.{ContestDateCalculator, ContestDates}
import upickle.default.*
import io.circe.Codec
import fdswarm.util.JavaTimePickle.given

import java.time.*

enum ContestType(val compute: Int => ContestDates) derives ReadWriter, Codec.AsObject:
  def dates(year: Int): ContestDates = compute(year)

  case WFD extends ContestType(ContestDateCalculator.lastFull)
  case ARRL extends ContestType(ContestDateCalculator.forthFullWeekend)

/**
 * What a user can choose in a dialog.
 * @param contest WFD or ARRL
 * @param start 1st day/time of contest
 * @param end last day/time of contest
 */
case class ContestConfig(contest:ContestType,
                         start:ZonedDateTime,
                         end:ZonedDateTime) derives ReadWriter, Codec.AsObject
case class ContestDetail(contest:ContestType,
                         start:ZonedDateTime,
                         end:ZonedDateTime,
                          classChars: String) derives ReadWriter, Codec.AsObject