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

import fdswarm.fx.qso.FdHour
import fdswarm.store.FdHourDigest
import fdswarm.util.NodeIdentity
import munit.FunSuite

import java.time.Instant

class SwarmStatusTest extends FunSuite:

  test("SwarmStatus.put should update nodeMap and NodeDetails"):
    val swarmStatus = new SwarmStatus
    val hp = NodeIdentity("192.168.1.100", 8080)
    val hour = FdHour(15, 12)
    val digest = FdHourDigest(hour, 10, "abc")
    val statusMessage = StatusMessage(Seq(digest))
    val nodeStuff = NodeStuff(statusMessage, hp)

    swarmStatus.put(nodeStuff)

    assert(swarmStatus.nodeMap.contains(hp))
    val nodeDetails = swarmStatus.nodeMap(hp)
    assert(nodeDetails.map.contains(hour))
    
    val cell = nodeDetails.map(hour)
    // We can't easily wait for Platform.runLater in a unit test without more setup,
    // but we can check if it was initialized at least, or if we can run the test in a way that handles Platform.
    // In many ScalaFX/JavaFX test environments, Platform.runLater might not execute immediately or at all without a Toolkit.
    
    // However, the FdHourNodeCell is created SYNC in NodeDetails.put (map.getOrElseUpdate)
    assertEquals(cell.nideIdentity, hp)
    assertEquals(cell.fdHour, hour)
    
    // The lhData update should have happened (either via Platform.runLater or fallback)
    assertEquals(cell.lhData.value.fdHourDigest, digest)
    assert(cell.lhData.value.lastSeen != Instant.EPOCH)
