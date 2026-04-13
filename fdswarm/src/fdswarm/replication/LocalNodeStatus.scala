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

import fdswarm.logging.LazyStructuredLogging
import fdswarm.StationConfigManager
import fdswarm.fx.bandmodes.SelectedBandModeManager
import fdswarm.fx.contest.ContestConfigManager
import fdswarm.model.BandModeOperator
import fdswarm.store.FdHourDigest
import fdswarm.util.NodeIdentityManager
import jakarta.inject.{Inject, Provider, Singleton}
import javafx.beans.property.{ReadOnlyObjectProperty, ReadOnlyObjectWrapper}

/**
 * Manages the status of the local node and publishes the latest value through a read-only object property.
 * This class is responsible
 * for maintaining and rebuilding the local node's status based on a combination of system configurations,
 * selected band modes, and contest configurations, as well as notifying listeners about the updates.
 *
 * The class interacts with several dependencies to compose the local node status:
 * - `NodeIdentityManager` provides the identity information for the local node.
 * - `StationConfigManager` facilitates access to station-specific configuration.
 * - `SelectedBandModeManager` manages the current selected band mode.
 * - `ContestConfigManager` provides access to the contest-specific configuration.
 *
 * The local node status is rebuilt and propagated under the following circumstances:
 * - When the station configuration changes.
 * - When the selected band mode changes.
 * - When the contest configuration becomes available or changes.
 * - When digests are explicitly updated via {@code updateDigests}.
 *
 * Thread Safety:
 * - The held digests and node status are maintained as volatile variables to ensure thread-safe access.
 *
 * Error Handling:
 * - If the contest configuration is unavailable during a status rebuild event, the rebuild is skipped,
 * and a debug log entry is generated for tracing purposes.
 *
 * @constructor Initializes the class with the required dependency injections managing node identity,
 *              station configuration, selected band mode, and contest configuration providers.
 * @param nodeIdentityManager          Manages and provides identity information for the local node.
 * @param stationManager               Handles station-specific configuration and triggers updates when changes occur.
 * @param selectedBandModeStore        Manages the current selected band mode and triggers updates upon changes.
 * @param contestConfigManagerProvider Provides access to contest configuration for generating node status.
 */
@Singleton
final class LocalNodeStatus @Inject()(
                                       nodeIdentityManager: NodeIdentityManager,
                                       stationManager: StationConfigManager,
                                       selectedBandModeStore: SelectedBandModeManager,
                                       contestConfigManagerProvider: Provider[ContestConfigManager]
                                     ) extends LazyStructuredLogging:

  @volatile private var heldDigests: Seq[FdHourDigest] = Nil
  private val currentBuffer: ReadOnlyObjectWrapper[NodeStatus] = new ReadOnlyObjectWrapper[NodeStatus](null)
  val current: ReadOnlyObjectProperty[NodeStatus] = currentBuffer.getReadOnlyProperty

  stationManager.stationProperty.onChange { (_, _, _) =>
    rebuildAndNotify("station-change")
  }
  selectedBandModeStore.selected.onChange { (_, _, _) =>
    rebuildAndNotify("bandmode-change")
  }

  // Rebuild local status when contest configuration changes.
  Option(
    contestConfigManagerProvider.get()
  ).foreach(
    _.contestConfigProperty.onChange {
      (_, _, _) =>
        rebuildAndNotify(
          "contest-config-change"
        )
    }
  )
  rebuildAndNotify("init")

  def updateDigests(digests: Seq[FdHourDigest]): Unit =
    heldDigests = digests
    rebuildAndNotify("digest-update")

  def update(nodeStatus: NodeStatus): Unit =
    currentBuffer.set(nodeStatus)

  private def rebuildAndNotify(reason: String): Unit =
    Option(contestConfigManagerProvider.get()).flatMap(_.contestConfigOption) match
      case Some(contestConfig) =>
        val bandNodeOperator =
          BandModeOperator(stationManager.station.operator, selectedBandModeStore.selected.value)
        val next = NodeStatus(
          statusMessage = StatusMessage(hash = heldDigests,, bandNodeOperator = bandNodeOperator, contestConfig = contestConfig),
          nodeIdentity = nodeIdentityManager.ourNodeIdentity,
          isLocal = true
        )
        update(next)
      case None =>
        logger.debug(s"Skipping local status rebuild ($reason): contest config not initialized")
