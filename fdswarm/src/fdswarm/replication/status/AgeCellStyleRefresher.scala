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

import com.typesafe.config.Config
import fdswarm.replication.NodeStatus
import fdswarm.util.{AgeStyleService, DurationFormat, NodeIdentity}
import jakarta.inject.{Inject, Singleton}
import scalafx.application.Platform
import scalafx.scene.Node
import scalafx.scene.control.{Label, Tooltip}

import java.time.{Duration, Instant, ZoneId}
import java.time.format.DateTimeFormatter
import java.util.Timer
import java.util.TimerTask
import scala.collection.concurrent.TrieMap

final case class CellStyleContext(
  nodeStatus: NodeStatus,
  fieldName: String,
  node: Node
)

@Singleton
class AgeCellStyleRefresher @Inject()(
  config: Config,
  ageStyleService: AgeStyleService
):
  private val nodeAgeStyleName = "nodeAging"
  private val styleClasses = Seq("fresh", "recent", "stale")
  private val receivedFormatter =
    DateTimeFormatter.ofPattern("MM-dd HH:mm:ss").withZone(ZoneId.systemDefault())
  private val contextByNode = TrieMap.empty[NodeIdentity, CellStyleContext]
  private val refreshPeriod = loadRefreshPeriod(
    config = config
  )
  private val timer = new Timer(
    "age-style-refresher",
    true
  )

  private val refreshMillis = math.max(
    1L,
    refreshPeriod.toMillis
  )
  timer.scheduleAtFixedRate(
    new TimerTask:
      override def run(): Unit =
        refreshAll()
    ,
    refreshMillis,
    refreshMillis
  )

  def add(
    cellStyleContext: CellStyleContext
  ): Unit =
    contextByNode.put(
      cellStyleContext.nodeStatus.nodeIdentity,
      cellStyleContext
    )
    refreshOne(
      cellStyleContext = cellStyleContext
    )

  def remove(
    nodeIdentity: NodeIdentity
  ): Unit =
    contextByNode.remove(
      nodeIdentity
    )

  private def loadRefreshPeriod(
    config: Config
  ): Duration =
    if config.hasPath("fdswarm.ageRefreshDuration") then
      config.getDuration("fdswarm.ageRefreshDuration")
    else
      Duration.ofSeconds(1L)

  private def refreshAll(): Unit =
    Platform.runLater(() =>
      contextByNode.values.foreach(
        context =>
          refreshOne(
            cellStyleContext = context
          )
      )
    )

  private def refreshOne(
    cellStyleContext: CellStyleContext
  ): Unit =
    if cellStyleContext.nodeStatus.isLocal then
      updateLabel(
        node = cellStyleContext.node,
        text = "Our Node",
        tooltipText = "Our, local, node is always current"
      )
      return

    val now = Instant.now()
    val styleAndAge = ageStyleService.calc(
      ageStyleName = nodeAgeStyleName,
      instant = cellStyleContext.nodeStatus.received,
      now = now
    )
    val text = DurationFormat(
      styleAndAge.age
    )
    val tooltipText = receivedFormatter.format(
      cellStyleContext.nodeStatus.received
    )
    updateLabel(
      node = cellStyleContext.node,
      text = text,
      tooltipText = tooltipText
    )
    val node = cellStyleContext.node
    node.styleClass.removeAll(
      styleClasses*
    )
    node.styleClass += styleAndAge.style

  private def updateLabel(
    node: Node,
    text: String,
    tooltipText: String
  ): Unit =
    node match
      case label: Label =>
        updateLabelValues(
          label = label,
          text = text,
          tooltipText = tooltipText
        )
      case _ =>
        node.delegate match
          case labelDelegate: javafx.scene.control.Label =>
            updateLabelValues(
              label = new Label(labelDelegate),
              text = text,
              tooltipText = tooltipText
            )
          case _ =>

  private def updateLabelValues(
    label: Label,
    text: String,
    tooltipText: String
  ): Unit =
    if label.text.isBound then
      label.text.unbind()
    label.text = text
    Option(
      label.delegate.getTooltip
    ) match
      case Some(existingTooltip) =>
        existingTooltip.setText(
          tooltipText
        )
      case None =>
        label.tooltip = new Tooltip(
          tooltipText
        )
