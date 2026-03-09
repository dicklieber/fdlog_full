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

import fdswarm.fx.qso.FdHour
import fdswarm.store.FdHourDigest
import fdswarm.util.NodeIdentity
import scalafx.application.Platform
import scalafx.beans.property.{IntegerProperty, ObjectProperty}

import java.time.Instant
import scala.collection.concurrent.TrieMap

class NodeDetails(val nodeIdentity: NodeIdentity):
  val map: TrieMap[FdHour, FdHourNodeCell] = new TrieMap[FdHour, FdHourNodeCell]
  val qsoCount: IntegerProperty = IntegerProperty(0)
  val lastUpdate: ObjectProperty[Instant] = ObjectProperty[Instant](Instant.EPOCH)

  def put(fdHourDigest: FdHourDigest, onUpdate: () => Unit): Unit =
    val cell = map.getOrElseUpdate(
      fdHourDigest.fdHour,
      FdHourNodeCell(nodeIdentity, fdHourDigest.fdHour)
    )
    val data = LHData(fdHourDigest, Instant.now())
    try
      Platform.runLater {
        cell.lhData.value = data
        recalculateQsoCount()
        lastUpdate.value = Instant.now()
        onUpdate()
      }
    catch
      case _: IllegalStateException =>
        // Fallback for tests or headless environments where Toolkit is not initialized
        cell.lhData.value = data
        recalculateQsoCount()
        lastUpdate.value = Instant.now()
        onUpdate()

  def recalculateQsoCount(): Unit =
    qsoCount.value = map.values.map(_.lhData.value.fdHourDigest.count).toSeq.sum
