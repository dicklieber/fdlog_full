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

  def pane(text: StringProperty): Pane =
    val grid = new GridPane {
      hgap = 10
      vgap = 5
    }
    val root = GridUtils.fieldSet("Possible Dups", grid)
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
            dups.zipWithIndex.foreach { (qso, index) =>
              val label = new Label(qso.callSign.toString) {
                styleClass += "dupCallsign"
                tooltip = Tooltip(localFrom(qso.stamp))
              }
              grid.add(label, index % 5, index / 5)
            }
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
