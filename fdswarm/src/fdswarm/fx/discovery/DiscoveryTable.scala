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

package fdswarm.fx.discovery

import com.typesafe.scalalogging.LazyLogging
import fdswarm.fx.contest.ContestConfigPaneProvider
import fdswarm.fx.utils.{GridColumn, GridColumnAlignment, GridColumnWidth, GridRowBehavior, TypedGridTableBuilder}
import scalafx.Includes.*
import scalafx.scene.Node
import scalafx.scene.control.Button
import scalafx.scene.layout.Region

class DiscoveryTable(contestConfigPane: ContestConfigPaneProvider#ContestConfigPane) extends LazyLogging:
  private type Ncs = NodeContestStation

  private def textCol(
                       header: String,
                       sortable: Boolean = false,
                       width: GridColumnWidth = GridColumnWidth.flexible(Region.USE_COMPUTED_SIZE),
                       alignment: GridColumnAlignment = GridColumnAlignment.Left,
                       cellStyleClasses: Ncs => Seq[String] = (_: Ncs) => Seq.empty[String]
                     )(
                       value: Ncs => String
                     ): GridColumn[Ncs] =
    GridColumn.text[Ncs](
      header = header,
      value = value,
      cellStyleClasses = cellStyleClasses,
      sortable = sortable,
      alignment = alignment,
      width = width
    )

  private def nodeCol(
                       header: String,
                       width: GridColumnWidth = GridColumnWidth.flexible(Region.USE_COMPUTED_SIZE),
                       alignment: GridColumnAlignment = GridColumnAlignment.Left,
                       cellStyleClasses: Ncs => Seq[String] = (_: Ncs) => Seq.empty[String],
                       sortValue: Option[Ncs => String] = None
                     )(
                       value: Ncs => Node
                     ): GridColumn[Ncs] =
    GridColumn.node[Ncs](
      header = header,
      value = value,
      cellStyleClasses = cellStyleClasses,
      sortValue = sortValue,
      alignment = alignment,
      width = width
    )

  private val table = TypedGridTableBuilder(
    GridRowBehavior[Ncs](
      rowStyleClasses = ncs =>
        if ncs.exchange.trim.isEmpty then Seq("row-warning") else Seq.empty,
      onClick = Some(ncs =>
        logger.info(s"Selected discovery row: $ncs")
      ),
      onDoubleClick = Some(ncs =>
        logger.info(s"Double-clicked discovery row: $ncs")
      )
    ),
    textCol(
      header = "Host IP",
      sortable = true,
      width = GridColumnWidth.fixed(140)
    )(_.nodeIdentity.hostIp),
    textCol(
      header = "Host Name",
      sortable = true,
      width = GridColumnWidth.flexible(180)
    )(_.nodeIdentity.hostName),
    textCol(
      header = "Port",
      sortable = true,
      alignment = GridColumnAlignment.Right,
      width = GridColumnWidth.fixed(80)
    )(_.nodeIdentity.port.toString),
    textCol(
      header = "Contest",
      sortable = true,
      width = GridColumnWidth.flexible(120)
    )(_.discoveryWire.contestConfig.contestType.toString),
    textCol(
      header = "Exchange",
      sortable = true,
      width = GridColumnWidth.flexible(120),
    )(_.exchange),
    textCol(
      header = "Our Call",
      sortable = true,
      width = GridColumnWidth.flexible(120)
    )(_.discoveryWire.contestConfig.ourCallsign.toString),
    textCol(
      header = "Operator",
      sortable = true,
      width = GridColumnWidth.flexible(150)
    )(_.discoveryWire.stationConfig.operator.toString),
    nodeCol(
      header = "Action",
      sortValue = Some(_.nodeIdentity.hostIp),
      alignment = GridColumnAlignment.Center,
      width = GridColumnWidth.fixed(110)
    ) { ncs =>
      new Button("Use"):
        styleClass += "grid-inline-button"
        onAction = _ => {
          contestConfigPane.update(ncs.discoveryWire.contestConfig)
          logger.info(s"Use clicked for ${ncs.nodeIdentity.hostIp}:${ncs.nodeIdentity.port}")
        }
    }
  )

  def grid: Node = table.grid

  def setItems(items: IterableOnce[Ncs]): Unit =
    table.setItems(items)