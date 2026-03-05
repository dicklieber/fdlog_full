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
import fdswarm.util.NodeIdentity
import jakarta.inject.*
import javafx.beans.value.ObservableObjectValue
import scalafx.application.Platform
import scalafx.beans.property.ObjectProperty
import scalafx.collections.ObservableMap

import java.time.Instant
import scala.collection.concurrent.TrieMap

@Singleton
class SwarmStatus @Inject() extends LazyLogging:
  val nodeMap: ObservableMap[NodeIdentity, NodeDetails] = ObservableMap[NodeIdentity, NodeDetails]()

  def put(nodeStuff: NodeStuff):Unit=
    for
      fdHourDigest <- nodeStuff.status.fdDigests
    do {
      val nodeIdentity = nodeStuff.nodeIdentity
      nodeMap.get(nodeIdentity) match
        case Some(nodeDetails) =>
          nodeDetails.put(fdHourDigest)
        case None =>
          val nodeDetails = NodeDetails(nodeIdentity)
          nodeDetails.put(fdHourDigest)
          try
            Platform.runLater {
              nodeMap.put(nodeIdentity, nodeDetails)
            }
          catch
            case _: IllegalStateException =>
              nodeMap.put(nodeIdentity, nodeDetails) //todo I don't know why this is needed.
    }


case class LHData(fdHourDigest: FdHourDigest, lastSeen: Instant = Instant.EPOCH)
case class FdHourNodeCell(nideIdentity: NodeIdentity, fdHour: FdHour):
  val lhData: ObjectProperty[LHData] =  ObjectProperty[LHData](LHData(FdHourDigest.empty(fdHour)))

class NodeDetails(nodeIdentity: NodeIdentity):
  val map: TrieMap[FdHour, FdHourNodeCell] = new TrieMap[FdHour, FdHourNodeCell]

  def put(fdHourDigest: FdHourDigest): Unit =
    val cell = map.getOrElseUpdate(fdHourDigest.fdHour, FdHourNodeCell(nodeIdentity, fdHourDigest.fdHour))
    val data = LHData(fdHourDigest, Instant.now())
    try
      Platform.runLater {
        cell.lhData.value = data
      }
    catch
      case _: IllegalStateException =>
        // Fallback for tests or headless environments where Toolkit is not initialized
        cell.lhData.value = data

