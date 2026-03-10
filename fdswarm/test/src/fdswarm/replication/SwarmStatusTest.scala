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

import fdswarm.TestDirectory
import fdswarm.fx.qso.FdHour
import fdswarm.replication.status.SwarmStatus
import fdswarm.store.FdHourDigest
import fdswarm.util.{MockNodeIdentityManager, NodeIdentity}
import munit.FunSuite

import java.time.Instant

class SwarmStatusTest extends FunSuite:

  test("SwarmStatus.put should update nodeMap and NodeDetails"):
    val testDir = new TestDirectory
    val swarmStatus = new SwarmStatus(testDir, MockNodeIdentityManager())
    val hp = NodeIdentity("192.168.1.100", 8080, "test-instance")
    val hour = FdHour(15, 12)
    val digest = FdHourDigest(hour, 10, "abc")
    val statusMessage = StatusMessage(Seq(digest))
    val nodeStuff = ReceivedNodeStatus(statusMessage, hp)

    swarmStatus.put(nodeStuff)

    assert(swarmStatus.nodeMap.contains(hp), "nodeMap should contain node identity")
    val nodeDetails = swarmStatus.nodeMap(hp)
    assert(nodeDetails.map.contains(hour), "nodeDetails should contain fdHour")
    
    val cell = nodeDetails.map(hour)
    
    // The lhData update should have happened (either via Platform.runLater or fallback)
    assertEquals(cell.lhData.value.fdHourDigest, digest)
    assert(cell.lhData.value.lastSeen != Instant.EPOCH, "lastSeen should be updated")
    
    assertEquals(nodeDetails.qsoCount.value, 10, "qsoCount should be updated")
    assert(nodeDetails.lastUpdate.value != Instant.EPOCH, "lastUpdate should be updated")

    testDir.cleanup()

  test("SwarmStatus should persist and reload state"):
    val testDir = new TestDirectory
    val hp = NodeIdentity("192.168.1.101", 9090, "test-instance-2")
    val hour = FdHour(16, 13)
    val digest = FdHourDigest(hour, 5, "def")
    val statusMessage = StatusMessage(Seq(digest))
    val nodeStuff = ReceivedNodeStatus(statusMessage, hp)

    // 1. Create SwarmStatus, put data, and it should save
    val swarmStatus1 = new SwarmStatus(testDir, MockNodeIdentityManager())
    swarmStatus1.put(nodeStuff)
    
    // 2. Create new SwarmStatus with same directory, it should load data
    val swarmStatus2 = new SwarmStatus(testDir, MockNodeIdentityManager())
    
    assert(swarmStatus2.nodeMap.contains(hp), "nodeMap should contain node identity after reload")
    val nodeDetails = swarmStatus2.nodeMap(hp)
    assert(nodeDetails.map.contains(hour), "nodeDetails should contain fdHour after reload")
    val cell = nodeDetails.map(hour)
    assertEquals(cell.lhData.value.fdHourDigest, digest)
    assertEquals(nodeDetails.qsoCount.value, 5, "qsoCount should be reloaded/recalculated")

    testDir.cleanup()
