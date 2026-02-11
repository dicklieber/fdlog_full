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
import fdswarm.fx.GridUtils
import fdswarm.fx.bandmodes.BandModeMatrixPane
import fdswarm.fx.sections.SectionPanel
import jakarta.inject.Singleton
import scalafx.geometry.Insets
import scalafx.scene.Node
import scalafx.scene.layout.{GridPane, VBox}
import GridUtils.*

@Singleton
class ContestEntry @Inject()(qsoEntryPanel: QsoEntryPanel,
                             qsoTablePane: QsoTablePane,
                             bandModeMatrixPane: BandModeMatrixPane,
                             sectionPanel: SectionPanel
                            ) extends LazyLogging:

  def node: Node =
    new GridPane {
      padding = Insets(10)
      hgap = 10
      vgap = 10
      add(qsoTablePane.node, 0, 0, 2, 1)
      add(qsoEntryPanel.node, 0, 1, 1, 1)
      add(sectionPanel.node, 1, 1, 1, 1)
      add(bandModeMatrixPane.node, 0, 2, 2, 1)
    }
//    new VBox {
//      children = Seq(
//        qsoTablePane.node,
//        qsoEntryPanel.node,
//        bandModeMatrixPane.node)
//
//    }