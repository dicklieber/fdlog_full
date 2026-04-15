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

import fdswarm.StationConfigManager
import fdswarm.fx.bandmodes.SelectedBandModeManager
import fdswarm.fx.contest.ContestConfigManager
import fdswarm.logging.LazyStructuredLogging
import fdswarm.logging.Locus.Replication
import fdswarm.model.BandModeOperator
import fdswarm.replication.status.SwarmData
import fdswarm.util.NodeIdentityManager
import jakarta.inject.{Inject, Singleton}
import javafx.beans.property.{ReadOnlyObjectProperty, ReadOnlyObjectWrapper}

/**
 * This class is responsible for building the [[StatusMessage]], sent to other nodes.
 * And [[NodeStatus]] that is [[fdswarm.replication.status.SwarmData]].
 */
@Singleton
final class LocalNodeStatus @Inject()(
                                       nodeIdentityManager: NodeIdentityManager,
                                       stationManager: StationConfigManager,
                                       selectedBandModeStore: SelectedBandModeManager,
                                       contestConfigManager: ContestConfigManager,
                                       swarmData: SwarmData
                                     ) extends LazyStructuredLogging(Replication):

  @volatile private var lastHashCount: HashCount = HashCount()

  def statusMessage:StatusMessage =
    throw new NotImplementedError("Not implemented yet.")

  def nodeStatus:NodeStatus =
    throw new NotImplementedError("Not implemented yet.")

  private val currentBuffer: ReadOnlyObjectWrapper[NodeStatus] = new ReadOnlyObjectWrapper[NodeStatus](null)
  val current: ReadOnlyObjectProperty[NodeStatus] = currentBuffer.getReadOnlyProperty

  stationManager.stationProperty.onChange { (_, _, _) =>
    rebuildAndNotify()
  }
  selectedBandModeStore.selected.onChange { (_, _, _) =>
    rebuildAndNotify()
  }

  rebuildAndNotify()

  def updateHashCount(hashCount: HashCount): Unit =
    lastHashCount = hashCount
    rebuildAndNotify()

  private def rebuildAndNotify(): Unit =
    val contestConfig = contestConfigManager.contestConfigProperty.value
    val bandNodeOperator = BandModeOperator(stationManager.station.operator, selectedBandModeStore.selected.value)
        val next = NodeStatus(
          statusMessage = StatusMessage(hashCount = lastHashCount,
            bandNodeOperator = bandNodeOperator,
            contestConfig = contestConfig),
          nodeIdentity = nodeIdentityManager.ourNodeIdentity,
          isLocal = true
        )
        swarmData.update(next)
