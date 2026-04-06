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
import fdswarm.StationConfigManager
import fdswarm.fx.bandmodes.SelectedBandModeManager
import fdswarm.fx.contest.ContestConfigManager
import fdswarm.model.BandModeOperator
import fdswarm.store.FdHourDigest
import fdswarm.util.{NodeIdentity, NodeIdentityManager}
import jakarta.inject.{Inject, Provider, Singleton}

@Singleton
final class LocalNodeStatus @Inject()(
                                       nodeIdentityManager: NodeIdentityManager,
                                       stationManager: StationConfigManager,
                                       selectedBandModeStore: SelectedBandModeManager,
                                       contestConfigManagerProvider: Provider[ContestConfigManager]
                                     ) extends LazyLogging:

  @volatile private var heldDigests: Seq[FdHourDigest] = Nil
  @volatile private var heldStatus: Option[NodeStatus] = None
  private var listeners: Vector[NodeStatus => Unit] = Vector.empty

  stationManager.stationProperty.onChange { (_, _, _) =>
    rebuildAndNotify("station-change")
  }
  selectedBandModeStore.selected.onChange { (_, _, _) =>
    rebuildAndNotify("bandmode-change")
  }

  // Rebuild local status once contest configuration becomes available.
  Option(contestConfigManagerProvider.get()).foreach(_.onConfigSet { _ =>
    rebuildAndNotify("contest-config-set")
  })
  rebuildAndNotify("init")

  def updateDigests(digests: Seq[FdHourDigest]): Unit =
    heldDigests = digests
    rebuildAndNotify("digest-update")

  def onUpdate(listener: NodeStatus => Unit): Unit =
    listeners = listeners :+ listener
    heldStatus.foreach(listener)

  def current: Option[NodeStatus] = heldStatus
  def ourNodeIdentity: NodeIdentity = nodeIdentityManager.ourNodeIdentity

  def update(nodeStatus: NodeStatus): Unit =
    heldStatus = Some(nodeStatus)
    listeners.foreach(_.apply(nodeStatus))

  private def rebuildAndNotify(reason: String): Unit =
    Option(contestConfigManagerProvider.get()).flatMap(_.contestConfigOption) match
      case Some(contestConfig) =>
        val bandNodeOperator =
          BandModeOperator(stationManager.station.operator, selectedBandModeStore.selected.value)
        val next = NodeStatus(
          statusMessage = StatusMessage(
            fdDigests = heldDigests,
            bandNodeOperator = bandNodeOperator,
            contestConfig = contestConfig
          ),
          nodeIdentity = nodeIdentityManager.ourNodeIdentity,
          isLocal = true
        )
        update(next)
      case None =>
        logger.debug(s"Skipping local status rebuild ($reason): contest config not initialized")
