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

package fdswarm.fx.qso

import com.google.inject.Inject
import com.typesafe.scalalogging.LazyLogging
import fdswarm.fx.GridColumns
import fdswarm.fx.bandmodes.BandModeMatrixPane
import fdswarm.fx.sections.SectionPanel
import jakarta.inject.Singleton
import scalafx.geometry.Insets
import scalafx.scene.Node
import scalafx.scene.layout.{ColumnConstraints, GridPane, Priority, VBox}
import fdswarm.fx.GridColumns.*

@Singleton
class ContestEntry @Inject()(qsoEntryPanel: QsoEntryPanel,
                             qsoTablePane: QsoTablePane,
                             qsoSearchPane: QsoSearchPane,
                             val bandModeMatrixPane: BandModeMatrixPane,
                             sectionPanel: SectionPanel,
                             contestTimerPanel: ContestTimerPanel,
                             contestManager: fdswarm.fx.contest.ContestConfigManager
                            ) extends LazyLogging:

  private val _node = new GridPane {
    padding = Insets(10)
    hgap = 10
    vgap = 10
    columnConstraints = Seq(
      new ColumnConstraints() { hgrow = Priority.Always },
      new ColumnConstraints() { hgrow = Priority.Never }
    )
  }

  private def buildUi(): Unit =
    _node.children.clear()
    // Row 0: Search pane above table
    _node.add(child = qsoSearchPane.node, columnIndex = 0, rowIndex = 0, colspan = 2, rowspan = 1)

    // Row 1: Table spans both columns
    _node.add(child = qsoTablePane.node, columnIndex = 0, rowIndex = 1, colspan = 2, rowspan = 1)

    // Row 2: Entry panel and Section panel
    _node.add(qsoEntryPanel.node, 0, 2)
    _node.add(sectionPanel.node, 1, 2, 1, 3) // Section panel spans 3 rows to match others

    // Row 3: Timer
    _node.add(contestTimerPanel.node, 0, 3)

    // Row 4: Band/Mode matrix
    _node.add(bandModeMatrixPane.node, 0, 4)
    bandModeMatrixPane.buildGrid()

  contestManager.hasConfiguration.onChange { (_, _, hasConfig) =>
    if hasConfig then buildUi()
    else _node.children.clear()
  }

  if contestManager.hasConfiguration.value then buildUi()

  def node: Node = _node
//    new VBox {
//      children = Seq(
//        qsoTablePane.node,
//        qsoEntryPanel.node,
//        bandModeMatrixPane.node)
//
//    }