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

import java.time.Instant
import io.circe.{Decoder, Encoder}

case class BandModeOperator(
    operator: Callsign,
    bandMode: BandMode,
    stamp: Instant = Instant.now()
)

object BandModeOperator:
  given Encoder[BandModeOperator] = Encoder.instance { bno =>
    import io.circe.syntax.*
    import fdswarm.util.JavaTimeCirce.given
    io.circe.Json.obj(
      "operator" -> bno.operator.asJson,
      "bandMode" -> bno.bandMode.asJson,
      "stamp" -> bno.stamp.asJson
    )
  }

  given Decoder[BandModeOperator] = Decoder.instance { c =>
    import fdswarm.util.JavaTimeCirce.given
    for
      operator <- c.downField("operator").as[Callsign]
      bandMode <- c.downField("bandMode").as[BandMode]
      stamp <- c.downField("stamp").as[Instant]
    yield BandModeOperator(operator, bandMode, stamp)
  }
