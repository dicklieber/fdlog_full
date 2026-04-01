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

package fdswarm.fx.tools

import fdswarm.fx.utils.*
import fdswarm.replication.{LiveOrDeadQueue, Transport}
import jakarta.inject.{Inject, Singleton}
import scalafx.Includes.*
import scalafx.application.Platform
import scalafx.geometry.Insets
import scalafx.scene.Node
import scalafx.scene.control.ButtonType
import scalafx.scene.layout.{Region, VBox}

import java.time.ZoneId
import java.time.format.DateTimeFormatter

private class UDPQueuesTable:
  private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault())

  private val table = TypedGridTableBuilder[LiveOrDeadQueue](
    GridRowBehavior.empty[LiveOrDeadQueue],
    GridColumn.text[LiveOrDeadQueue](
      header = "Service",
      value = _.service.toString,
      sortable = true,
      width = GridColumnWidth.flexible(120)
    ),
    GridColumn.text[LiveOrDeadQueue](
      header = "isAlive",
      value = _.isAlive.toString,
      sortable = true,
      width = GridColumnWidth.fixed(80),
      alignment = GridColumnAlignment.Center
    ),
    GridColumn.text[LiveOrDeadQueue](
      header = "Queue Size",
      value = _.size.toString,
      sortable = true,
      width = GridColumnWidth.fixed(100),
      alignment = GridColumnAlignment.Right
    ),
    GridColumn.text[LiveOrDeadQueue](
      header = "Started",
      value = q => timeFormatter.format(q.started),
      sortable = true,
      width = GridColumnWidth.flexible(100)
    )
  )

  def grid: Node = table.grid

  def setItems(items: Iterable[LiveOrDeadQueue]): Unit =
    table.setItems(items)

@Singleton
class UDPQueuesDialog @Inject() (transport: Transport)
  extends StyledDialog[ButtonType]:

  private val udpQueuesTable = new UDPQueuesTable()

  title = "UDP Queues"
  headerText = "Live or Dead UDP Queues"
  
  private val vBox = new VBox {
    spacing = 10
    padding = Insets(10)
    children = Seq(udpQueuesTable.grid)
  }

  dialogPane().content = vBox
  dialogPane().buttonTypes = Seq(ButtonType.Close)

  onShowing = _ => {
    refresh()
  }

  def refresh(): Unit =
    udpQueuesTable.setItems(transport.queues.values)

  def show(): Unit =
    refresh()
    showAndWait()
