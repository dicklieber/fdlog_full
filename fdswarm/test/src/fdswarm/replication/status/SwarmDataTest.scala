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
import munit.FunSuite

class SwarmDataTest extends FunSuite:
  test("resolveFields with FdHours expands known and newly discovered hours"):
    val swarmData = SwarmData()
    val hour1 = FdHour(10, 1)
    val hour2 = FdHour(10, 2)

    val fields = Seq(NodeDataField.HostName, FdHours)

    swarmData.addKnownFdHours(Seq(hour1))
    val resolvedAfterFirstUpdate = swarmData.resolveFields(fields)
    assert(resolvedAfterFirstUpdate.contains(NodeDataField.HostName))
    assert(resolvedAfterFirstUpdate.contains(NodeDataField.FdHoursField(FdHours(hour1))))
    assert(!resolvedAfterFirstUpdate.contains(NodeDataField.FdHoursField(FdHours(hour2))))

    swarmData.addKnownFdHours(Seq(hour1, hour2))
    val resolvedAfterSecondUpdate = swarmData.resolveFields(fields)
    assert(resolvedAfterSecondUpdate.contains(NodeDataField.HostName))
    assert(resolvedAfterSecondUpdate.contains(NodeDataField.FdHoursField(FdHours(hour1))))
    assert(resolvedAfterSecondUpdate.contains(NodeDataField.FdHoursField(FdHours(hour2))))
