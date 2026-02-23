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

import com.typesafe.scalalogging.LazyLogging
import fdswarm.fx.GridUtils
import fdswarm.fx.bandmodes.{BandModeStore, SelectedBandModeStore}
import fdswarm.store.QsoStore
import fdswarm.util.TimeHelpers.localFrom
import jakarta.inject.{Inject, Singleton}
import scalafx.application.Platform
import scalafx.beans.property.StringProperty
import scalafx.scene.control.{Label, Tooltip}
import scalafx.scene.layout.{GridPane, Pane, StackPane, VBox}

class DupPanel @Inject()(
                          qsoStore: QsoStore,
                          bandModeStore: BandModeStore,
                          selectedBandModeStore: SelectedBandModeStore
                        ) extends LazyLogging:

  private val grid = new GridPane {
    hgap = 10
    vgap = 5
  }
  private var root: Pane = _

  def showDuplicateError(qso: fdswarm.model.Qso): Unit =
    Platform.runLater {
      grid.children.clear()
      
      val label = new Label(qso.rejectedMsg) {
        styleClass += "dup-error-label"
        style = "-fx-text-fill: red; -fx-font-weight: bold;"
      }
      grid.add(label, 0, 0, 9, 1)
      root.visible = true
      root.managed = true
    }

  def pane(text: StringProperty): Pane =
    root = GridUtils.fieldSet("Possible Dups", grid)
    root.visible = false
    root.managed = false

    text.onChange { (_, _, newValue) =>
      if newValue != null && newValue.length > 1 then
        val bandMode = selectedBandModeStore.selected.value
        val dups = qsoStore.potentialDups(newValue, bandMode)
        logger.debug(s"Potential dups for $newValue: $dups")

        Platform.runLater {
          if dups.isEmpty then
            root.visible = false
            root.managed = false
          else
            grid.children.clear()
            val displayedDups = dups.take(45)
            displayedDups.zipWithIndex.foreach { (qso, index) =>
              val label = new Label(qso.callsign.toString) {
                styleClass += "dupCallsign"
                tooltip = Tooltip(localFrom(qso.stamp))
              }
              grid.add(label, index % 9, index / 9)
            }
            if dups.size > 45 then
              val moreLabel = new Label(s"${dups.size - 45} more possible dups") {
                styleClass +=  "dupCallsignMore"
              }
              // Place in the next row, spanning all columns
              val nextRow = (displayedDups.size + 8) / 9
              grid.add(moreLabel, 0, nextRow, 9, 1) 
            root.visible = true
            root.managed = true
        }
      else
        Platform.runLater {
          root.visible = false
          root.managed = false
        }
    }

    root
