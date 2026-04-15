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

package fdswarm.replication.status

import com.google.inject.name.Named
import fdswarm.fx.GridColumns
import jakarta.inject.{Inject, Singleton}
import scalafx.application.Platform
import scalafx.scene.control.Label
import scalafx.scene.layout.BorderPane

import java.util.concurrent.atomic.AtomicLong

@Singleton
class NodeBandOpPane @Inject()(
                                swarmData: SwarmData,
                                @Named("fdswarm.nodeBandOpRefreshSeconds") nodeBandOpRefreshSeconds: Int
                              ):

  private val refreshIntervalMillis = math.max(0L, nodeBandOpRefreshSeconds.toLong * 1000L)
  private val lastRefreshMillis = AtomicLong(0L)

  private val titleLabel = new Label()
  private val contentPane = new BorderPane:
    center = buildGrid()
  val node = GridColumns.fieldSet(
    titleLabel,
    contentPane
  )
  private val removeNodeStatusListener = swarmData.addNodeStatusListener(
    statuses =>
      Platform.runLater {
        updateTitle(
          statuses.size
        )
      }
  )

  def refresh(): Unit =
    refreshInternal(force = true)

  def refreshIfDue(): Unit =
    refreshInternal(force = false)

  private def refreshInternal(force: Boolean): Unit =
    val now = System.currentTimeMillis()
    if force then
      lastRefreshMillis.set(now)
      Platform.runLater {
        contentPane.center = buildGrid()
        updateTitle(
          swarmData.allNodeStatuses.size
        )
      }
    else if markRefreshDue(now) then
      Platform.runLater {
        contentPane.center = buildGrid()
        updateTitle(
          swarmData.allNodeStatuses.size
        )
      }

  private def markRefreshDue(now: Long): Boolean =
    if refreshIntervalMillis <= 0 then return true
    var retry = true
    while retry do
      val last = lastRefreshMillis.get()
      if now - last < refreshIntervalMillis then return false
      if lastRefreshMillis.compareAndSet(last, now) then return true
    false

  private def buildGrid() =
    swarmData.buildGridPane(
      Seq(
        NodeDataField.HostName,
        NodeDataField.Operator,
        NodeDataField.BandMode
      )
    )

  private def updateTitle(
                           nodeCount: Int
                         ): Unit =
    titleLabel.text =
      if nodeCount == 1 then "Swarm"
      else s"Swarm ($nodeCount nodes)"
