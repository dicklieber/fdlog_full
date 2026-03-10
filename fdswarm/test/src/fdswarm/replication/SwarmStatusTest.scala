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

  test("SwarmStatus.put should update nodeMap"):
    val testDir = new TestDirectory
    val swarmStatus = new SwarmStatus(testDir, MockNodeIdentityManager(), null)
    val hp = NodeIdentity("192.168.1.100", 8080, "test-instance")
    val hour = FdHour(15, 12)
    val digest = FdHourDigest(hour, 10, "abc")
    val statusMessage = StatusMessage(Seq(digest))
    val nodeStuff = ReceivedNodeStatus(statusMessage, hp)

    swarmStatus.put(nodeStuff)

    assert(swarmStatus.nodeMap.contains(hp), "nodeMap should contain node identity")
    val receivedStatus = swarmStatus.nodeMap(hp)
    assertEquals(receivedStatus.qsoCount, 10, "qsoCount should be 10")

    testDir.cleanup()

  test("SwarmStatus should persist state"):
    val testDir = new TestDirectory
    val hp = NodeIdentity("192.168.1.101", 9090, "test-instance-2")
    val hour = FdHour(16, 13)
    val digest = FdHourDigest(hour, 5, "def")
    val statusMessage = StatusMessage(Seq(digest))
    val nodeStuff = ReceivedNodeStatus(statusMessage, hp)

    // 1. Create SwarmStatus, put data, and it should save
    val swarmStatus1 = new SwarmStatus(testDir, MockNodeIdentityManager(), null)
    swarmStatus1.put(nodeStuff)
    
    // 2. verify file exists
    assert(os.exists(testDir() / "swarmStatus.json"))

    testDir.cleanup()
