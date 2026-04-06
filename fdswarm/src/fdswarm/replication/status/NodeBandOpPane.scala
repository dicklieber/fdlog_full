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
import fdswarm.fx.GridBuilder
import fdswarm.fx.station.StationEditor
import jakarta.inject.{Inject, Singleton}
import scalafx.Includes.*
import scalafx.application.Platform
import scalafx.scene.control.{Label, TitledPane, Tooltip}

import java.util.concurrent.atomic.AtomicLong

@Singleton
class NodeBandOpPane @Inject()(swarmStatus: SwarmStatus,
                               stationEditor: StationEditor,
                               @Named("fdswarm.nodeBandOpRefreshSeconds") nodeBandOpRefreshSeconds: Int):

  private val refreshIntervalMillis = math.max(0L, nodeBandOpRefreshSeconds.toLong * 1000L)
  private val lastRefreshMillis = AtomicLong(0L)

  val node: TitledPane = new TitledPane {
    text = "Swarm"
    collapsible = false
    content = buildGrid()
  }

  def refresh(): Unit =
    refreshInternal(force = true)

  def refreshIfDue(): Unit =
    refreshInternal(force = false)

  private def refreshInternal(force: Boolean): Unit =
    val now = System.currentTimeMillis()
    if force then
      lastRefreshMillis.set(now)
      Platform.runLater {
        node.content = buildGrid()
      }
    else if markRefreshDue(now) then
      Platform.runLater {
        node.content = buildGrid()
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
    val builder = GridBuilder()
    val nodes = swarmStatus.nodeMap.toSeq.sortBy(_._2)
    val ourNodeColumnIndex = nodes.indexWhere(_._1 == swarmStatus.ourNodeIdentity)
    if ourNodeColumnIndex >= 0 then
      builder.setColumnClass(ourNodeColumnIndex + 1, "ourNode")

//    builder("id", nodes.map(_._1)*)
    builder(
      "operator",
      nodes.zipWithIndex.map((entry, idx) =>
        operatorCallsignEditor(entry._2.statusMessage.bandNodeOperator.operator.toString, idx == ourNodeColumnIndex)
      )*
    )
    builder(
      "bandMode",
      nodes.zipWithIndex.map((entry, idx) =>
        new Label(entry._2.statusMessage.bandNodeOperator.bandMode.toString)
      )*
    )
    builder("hostName", nodes.map(_._1.hostName)*)

    builder.result

  private def operatorCallsignEditor(value: String, isLocalNode: Boolean): Label =
    val label = new Label(value)
    if isLocalNode then
      label.tooltip = new Tooltip("Change this node's operator.")
      label.style = "-fx-cursor: hand;"
      label.onMouseClicked = _ =>
        Option(node.scene.value)
          .flatMap(scene => Option(scene.window.value))
          .foreach(w => stationEditor.show(w))
    label
