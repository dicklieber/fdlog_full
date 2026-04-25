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

class MockNodeIdentityManager(val mockNodeIdentity: NodeIdentity, instanceIdManager: InstanceIdManager = null) extends NodeIdentityManager(mockNodeIdentity.port, instanceIdManager):

  override def suitableInterfaces: Seq[AnIpAddress] = Seq(AnIpAddress("mock", mockNodeIdentity.hostIp))
  override def currentIp: AnIpAddress = AnIpAddress("mock", mockNodeIdentity.hostIp)

  override def hostPort: String = s"${mockNodeIdentity.hostIp}:${mockNodeIdentity.port}"
  override def ourNodeIdentity: NodeIdentity = mockNodeIdentity
  override def isUs(nodeIdentity: NodeIdentity): Boolean =
    nodeIdentity.instanceId == mockNodeIdentity.instanceId

object MockNodeIdentityManager:
  def apply(host: String = "127.0.0.1", port: Int = 8080): MockNodeIdentityManager =
    new MockNodeIdentityManager(mockNodeIdentity = NodeIdentity(host, port, hostName = "ccc", instanceId = "iii"))
