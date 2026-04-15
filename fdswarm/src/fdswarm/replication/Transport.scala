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

import fdswarm.util.{NodeIdentity, NodeIdentityManager}

import java.util.concurrent.LinkedBlockingQueue

trait Transport:
  val nodeIdentityManager: NodeIdentityManager
  def isUs(
    candidate: NodeIdentity
  ): Boolean =
    nodeIdentityManager.isUs(candidate)
  val mode: String
  val incomingQueue: LinkedBlockingQueue[UDPHeaderData] =
    new LinkedBlockingQueue[UDPHeaderData]()

  /**
   * Just send a message of type [[Service]] with a given payload.
   */
  def send(service: Service, data: Array[Byte]): Unit
  def stop(): Unit
