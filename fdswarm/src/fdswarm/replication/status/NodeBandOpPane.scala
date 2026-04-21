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
import fdswarm.fx.utils.{IconButton, StyledDialog}
import fdswarm.replication.{Service, Transport}
import io.circe.syntax.EncoderOps
import jakarta.inject.{Inject, Singleton}
import scalafx.Includes.*
import scalafx.application.Platform
import scalafx.beans.binding.Bindings
import scalafx.collections.ObservableBuffer
import scalafx.geometry.{Insets, Pos}
import scalafx.scene.control.{Button, ButtonType, ComboBox, Label}
import scalafx.scene.layout.{BorderPane, HBox, Priority, VBox}
import scalafx.scene.paint.Color

import java.nio.charset.StandardCharsets

import java.util.concurrent.atomic.AtomicLong

@Singleton
class NodeBandOpPane @Inject() (
    swarmData: SwarmData,
    transport: Transport,
    @Named("fdswarm.nodeBandOpRefreshSeconds")
    nodeBandOpRefreshSeconds: Int):

  private val titleLabel = new Label()
  private val contestConfigWarningButton = IconButton(
    name = "exclamation-triangle",
    size = 18,
    tooltipText = "Not all nodes have the same Contest Configuration",
    color = Color.Red
  )
  contestConfigWarningButton.visible <== swarmData.contestConfigMismatchProperty
  contestConfigWarningButton.managed <== contestConfigWarningButton.visible
  contestConfigWarningButton.onAction = _ => showContestConfigMismatchDialog()
  private val toolbar = new HBox:
    alignment = Pos.CenterRight
    padding = Insets(2, 2, 2, 2)
    children = Seq(
      contestConfigWarningButton
    )
  private val contentPane = new BorderPane:
    top = toolbar
    center = buildGrid()
  private val titleBinding = Bindings.createStringBinding(
    () =>
      val nodeCount = swarmData.size.value
      if nodeCount == 1 then "Swarm" else s"Swarm ($nodeCount nodes)",
    swarmData.size
  )
  titleLabel.text <== titleBinding
  val node = GridColumns.fieldSet(
    titleLabel,
    contentPane
  )
  private val refreshIntervalMillis = math.max(0L, nodeBandOpRefreshSeconds.toLong * 1000L)
  private val lastRefreshMillis = AtomicLong(0L)

  def refresh(): Unit = refreshInternal(force = true)

  private def refreshInternal(force: Boolean): Unit =
    val now = System.currentTimeMillis()
    if force then
      lastRefreshMillis.set(now)
      Platform.runLater {
        contentPane.center = buildGrid()
      }
    else if markRefreshDue(now) then
      Platform.runLater {
        contentPane.center = buildGrid()
      }

  private def markRefreshDue(now: Long): Boolean =
    if refreshIntervalMillis <= 0 then return true
    var retry = true
    while retry do
      val last = lastRefreshMillis.get()
      if now - last < refreshIntervalMillis then return false
      if lastRefreshMillis.compareAndSet(last, now) then return true
    false

  private def buildGrid() = swarmData
    .buildGridPane(
      Seq(
        NodeDataField.HostName,
        NodeDataField.Operator,
        NodeDataField.BandMode
      )
    )

  def refreshIfDue(): Unit = refreshInternal(force = false)

  private def showContestConfigMismatchDialog(): Unit =
    val allStatuses = swarmData
      .allNodeStatuses
      .sorted
    if allStatuses.isEmpty then return

    val options = allStatuses.map(nodeStatus => nodeStatus.nodeIdentity)
    val optionLabels = options.map(nodeIdentity =>
      s"${nodeIdentity.hostName} (${nodeIdentity.instanceId})"
    )
    val optionByLabel = optionLabels.zip(options).toMap
    val sourceNodeSelector = new ComboBox[String](
      ObservableBuffer.from(
        optionLabels
      )
    ):
      value = optionLabels.headOption.orNull
      hgrow = Priority.Always

    val syncButton = new Button("Broadcast Selected Contest Config"):
      onAction = _ =>
        Option(sourceNodeSelector.value.value)
          .flatMap(label => optionByLabel.get(label))
          .flatMap(nodeIdentity =>
            swarmData.nodeMap
              .get(nodeIdentity)
              .map(nodeStatus => nodeStatus.statusMessage.contestConfig)
          )
          .foreach(contestConfig =>
            val payload = contestConfig
              .asJson
              .noSpaces
              .getBytes(
                StandardCharsets.UTF_8
              )
            transport.send(
              service = Service.SyncContest,
              data = payload
            )
          )

    val dialog = new StyledDialog[ButtonType]:
      title = "Contest Configuration Mismatch"
      headerText = "Not all nodes have the same Contest Configuration"
      resizable = true
    dialog.dialogPane().buttonTypes = Seq(
      ButtonType.Close
    )
    dialog.dialogPane().content = new VBox:
      spacing = 8
      padding = Insets(10)
      children = Seq(
        swarmData.buildGridPane(
          fields = Seq(
            NodeDataField.HostName,
            NodeDataField.ContestType,
            NodeDataField.ContestCallsign,
            NodeDataField.Exchange
          )
        ),
        new HBox:
          spacing = 8
          alignment = Pos.CenterRight
          children = Seq(
            new Label("Source Node"),
            sourceNodeSelector,
            syncButton
          )
      )
    dialog.showAndWait()
