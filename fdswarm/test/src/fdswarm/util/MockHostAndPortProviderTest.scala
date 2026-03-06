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

class MockHostAndPortProviderTest extends FunSuite:
  test("MockHostAndPortProvider should supply correct nodeIdentity"):
    val ni = NodeIdentity("1.2.3.4", 9999, "my-instance")
    val mock = new MockHostAndPortProvider(ni)
    
    assertEquals(mock.nodeIdentity, ni)
    assertEquals(mock.currentIp.ip, "1.2.3.4")
    assertEquals(mock.portAndInstance.port, 9999)
    assertEquals(mock.portAndInstance.instanceId, "my-instance")
    assertEquals(mock.hostPort, "1.2.3.4:9999")

  test("MockHostAndPortProvider.apply should work"):
    val mock = MockHostAndPortProvider("5.6.7.8", 1234)
    assertEquals(mock.nodeIdentity.host, "5.6.7.8")
    assertEquals(mock.nodeIdentity.port, 1234)
