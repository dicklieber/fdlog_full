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
import fdswarm.model.Callsign
import io.circe.Codec
import fdswarm.util.JavaTimeCirce.given

import java.time.*

enum ContestType(val name: String, val compute: Int => ContestDates) derives sttp.tapir.Schema:
  def dates(year: Int): ContestDates = compute(year)

  case WFD extends ContestType("Winter Field Day", ContestDateCalculator.lastFull)
  case ARRL extends ContestType("ARRL Field Day", ContestDateCalculator.forthFullWeekend)

object ContestType:
  import io.circe.{Decoder, Encoder}
  given Codec[ContestType] = Codec.from(
    Decoder.decodeString.emap(s =>
      try Right(ContestType.valueOf(s))
      catch case _: IllegalArgumentException => Left(s"Invalid ContestType: $s")
    ),
    Encoder.encodeString.contramap(_.toString)
  )

/**
 * What a user can choose in a dialog.
 * @param contest WFD or ARRL
 * @param start 1st day/time of contest
 * @param end last day/time of contest
 */
case class ContestConfig(contest: ContestType,
                         start: ZonedDateTime,
                         end: ZonedDateTime,
                         ourCallsign: Callsign,
                         transmitters: Int,
                         ourClass: String,
                         ourSection: String,
                         stamp: Instant = Instant.now()) derives Codec.AsObject