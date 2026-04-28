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

package fdswarm.replication

import fdswarm.contestStart.ContestStart
import fdswarm.fx.contest.ContestConfig
import fdswarm.model.Qso
import io.circe.Decoder

enum Service[T](
  using private val payloadDecoder: Decoder[T]
):
  type Payload = T
  case Status extends Service[StatusMessage]
  case SendStatus extends Service[NoPayload]
  case QSO extends Service[Qso]
  case SyncContest extends Service[ContestConfig]
  case ContestStart extends Service[ContestStart]

  def decode(
    udpHeaderData: UDPHeaderData
  ): T =
    udpHeaderData.decodePayload[T](
      using payloadDecoder
    )

final case class NoPayload()

object NoPayload:
  given Decoder[NoPayload] = Decoder.const(
    NoPayload()
  )
