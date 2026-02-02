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
import fdswarm.fx.bandmodes.BandModeMatrixPane
import fdswarm.model.ContestId
import jakarta.inject.Singleton
import scalafx.scene.Node
import scalafx.scene.layout.VBox

@Singleton
class ContestEntry @Inject() (qsoEntryPanel:QsoEntryPanel,
                              qsoTablePane: QsoTablePane,
                              bandModeMatrixPane: BandModeMatrixPane) extends LazyLogging:

  def node:Node =
    new VBox {
      children = Seq(
        qsoTablePane.node,
        qsoEntryPanel.node,
        bandModeMatrixPane.node)

    }