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

import jakarta.inject.{Inject, Named}

class MockHostAndPortProvider(val mockNodeIdentity: NodeIdentity) extends HostAndPortProvider(mockNodeIdentity.port):
  
  override def suitableInterfaces: Seq[AnIpAddress] = Seq(AnIpAddress("mock", mockNodeIdentity.host))
  override def currentIp: AnIpAddress = AnIpAddress("mock", mockNodeIdentity.host)
  override def setIp(newIp: AnIpAddress): Unit = ()

  override def hostPort: String = s"${mockNodeIdentity.host}:${mockNodeIdentity.port}"
  override def nodeIdentity: NodeIdentity = mockNodeIdentity
  override def portAndInstance: PortAndInstance = PortAndInstance(mockNodeIdentity.port, mockNodeIdentity.instanceId)

object MockHostAndPortProvider:
  def apply(host: String = "127.0.0.1", port: Int = 8080): MockHostAndPortProvider =
    new MockHostAndPortProvider(NodeIdentity(host, port))
