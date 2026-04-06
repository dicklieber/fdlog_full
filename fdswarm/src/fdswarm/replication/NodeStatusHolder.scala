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

import fdswarm.store.FdHourDigest
import fdswarm.util.NodeIdentity
import scalafx.beans.property.{BooleanProperty, ObjectProperty, StringProperty}
import scalafx.collections.ObservableBuffer

import java.time.Instant

/**
 * Holds NodeStatus fields as observable properties for UI consumers.
 */
final class NodeStatusHolder(val nodeIdentity: NodeIdentity):
  val received: ObjectProperty[Instant] = ObjectProperty[Instant](Instant.EPOCH)
  val bandNode: StringProperty = StringProperty("")
  val operator: StringProperty = StringProperty("")
  val fdHours: ObservableBuffer[ObjectProperty[FdHourDigest]] =
    ObservableBuffer.empty[ObjectProperty[FdHourDigest]]
  val fdHoursSizeChanged: BooleanProperty = BooleanProperty(false)

  @volatile private var heldStatus: Option[NodeStatus] = None

  def current: Option[NodeStatus] = heldStatus

  def update(nodeStatus: NodeStatus): Unit =
    heldStatus = Some(nodeStatus)
    received.value = nodeStatus.received
    bandNode.value = nodeStatus.statusMessage.bandNodeOperator.bandMode.toString
    operator.value = nodeStatus.statusMessage.bandNodeOperator.operator.toString

    val previousSize = fdHours.size
    fdHours.clear()
    nodeStatus.statusMessage.fdDigests.foreach(fdHourDigest =>
      fdHours += ObjectProperty[FdHourDigest](fdHourDigest)
    )
    fdHoursSizeChanged.value = previousSize != fdHours.size
