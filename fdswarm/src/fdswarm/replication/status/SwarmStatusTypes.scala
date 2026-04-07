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

package fdswarm.replication.status

import fdswarm.fx.qso.FdHour
import fdswarm.store.FdHourDigest
import fdswarm.util.{JavaTimeCirce, NodeIdentity}
import io.circe.Codec
import scalafx.beans.property.ObjectProperty

import java.time.Instant

case class LHData(
  fdHourDigest: FdHourDigest,
  lastSeen: Instant = Instant.EPOCH
)

object LHData:
  import JavaTimeCirce.given

  given Codec[LHData] = Codec.AsObject.derived

case class FdHourNodeCellDTO(
  fdHour: FdHour,
  lhData: LHData
) derives Codec.AsObject

case class FdHourNodeCell(
  nideIdentity: NodeIdentity,
  fdHour: FdHour
):
  val lhData: ObjectProperty[LHData] =
    ObjectProperty[LHData](
      LHData(
        FdHourDigest.empty(fdHour)
      )
    )
