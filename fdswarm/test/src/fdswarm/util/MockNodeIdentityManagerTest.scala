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

package fdswarm.util

import munit.FunSuite

class MockNodeIdentityManagerTest extends FunSuite:
  test("MockNodeIdentityManager should supply correct nodeIdentity"):
    val ni = NodeIdentity("1.2.3.4", 9999, "my-instance", "sss")
    val mock = new MockNodeIdentityManager(ni)
    
    assertEquals(mock.ourNodeIdentity, ni)
    assertEquals(mock.currentIp.ip, "1.2.3.4")
    assertEquals(mock.hostPort, "1.2.3.4:9999")

  test("MockNodeIdentityManager.apply should work"):
    val mock = MockNodeIdentityManager("5.6.7.8", 1234)
    assertEquals(mock.ourNodeIdentity.hostIp, "5.6.7.8")
    assertEquals(mock.ourNodeIdentity.port, 1234)
