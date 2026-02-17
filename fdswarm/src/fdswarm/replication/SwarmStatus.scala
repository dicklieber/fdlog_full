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

import com.typesafe.scalalogging.LazyLogging
import fdswarm.fx.qso.FdHour
import fdswarm.store.FdHourDigest
import fdswarm.util.HostAndPort
import jakarta.inject.*
import javafx.beans.value.ObservableObjectValue
import scalafx.application.Platform
import scalafx.beans.property.ObjectProperty
import scalafx.collections.ObservableMap

import java.time.Instant
import scala.collection.concurrent.TrieMap

@Singleton
class SwarmStatus @Inject()() extends LazyLogging:
  val nodeMap: ObservableMap[HostAndPort, NodeDetails] = ObservableMap[HostAndPort, NodeDetails]()

  def put(statusMessage:StatusMessage):Unit=
    val hostAndPort = statusMessage.hostAndPort
    for
      fdHourDigest <- statusMessage.fdDigests
    do
      nodeMap.get(hostAndPort) match
        case Some(nodeDetails) =>
          nodeDetails.put(fdHourDigest)
        case None =>
          val nodeDetails = NodeDetails(hostAndPort)
          nodeDetails.put(fdHourDigest)
          nodeMap.put(hostAndPort, nodeDetails)


case class LHData(fdHourDigest: FdHourDigest, lastSeen: Instant = Instant.EPOCH)
case class FdHourNodeCell(hostAndPort: HostAndPort, fdHour:FdHour):
  val lhData: ObjectProperty[LHData] =  ObjectProperty[LHData](LHData(FdHourDigest.empty(fdHour)))

class NodeDetails(hostAndPort: HostAndPort):
  val map: TrieMap[FdHour, FdHourNodeCell] = new TrieMap[FdHour, FdHourNodeCell]

  def put(fdHourDigest: FdHourDigest): Unit =
    val cell = map.getOrElseUpdate(fdHourDigest.fdHour, FdHourNodeCell(hostAndPort, fdHourDigest.fdHour))
    val data = LHData(fdHourDigest, Instant.now())
    try
      Platform.runLater {
        cell.lhData.value = data
      }
    catch
      case _: IllegalStateException =>
        // Fallback for tests or headless environments where Toolkit is not initialized
        cell.lhData.value = data

