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
import io.circe.Encoder
import io.circe.syntax.*

import java.nio.charset.StandardCharsets
import scala.collection.concurrent.TrieMap

trait Transport:
  val nodeIdentityManager: NodeIdentityManager
  def isUs(candidate:NodeIdentity):Boolean=
    nodeIdentityManager.isUs(candidate)
  val mode: String
  def startQueue(service: Service): LiveOrDeadQueue
  def stopQueue(service: Service): Unit=
    queues.get(service).foreach(_.invalidateQueue())
  
  def send(service: Service, data: Array[Byte] = Array.empty): Unit
  def send[T](service: Service, payload: T)(using Encoder[T]): Unit =
    send(service, payload.asJson.noSpaces.getBytes(StandardCharsets.UTF_8))
  def sentCount: Long
  def stop(): Unit
  val queues: TrieMap[Service, LiveOrDeadQueue] = new TrieMap[Service, LiveOrDeadQueue]()

