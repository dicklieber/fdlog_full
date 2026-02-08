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

import fdswarm.ContestDates
import upickle.default.*
import fdswarm.util.JavaTimePickle.given_ReadWriter_ZonedDateTime

import java.time.{LocalDate, LocalDateTime, ZonedDateTime}
case class ContestId(content:ContestType, year:Int) derives ReadWriter

enum ContestType derives ReadWriter:
  case WFD, ARRL

/**
 *
 * @param contest which one
 * @param classChars e.g. "HIOM" (for WFD) or "ACBDE" etc. (for ARRL)
 * @param start when contest started.
 * @param end when contest ends.
 */
case class ContestDetail(contest:ContestType,
                         classChars:String,
                         start:ZonedDateTime,
                         end:ZonedDateTime) derives ReadWriter