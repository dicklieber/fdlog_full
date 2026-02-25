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
import fdswarm.store.{DupInfo, QsoStore, StyledMessage}
import fdswarm.util.TimeHelpers.localFrom
import jakarta.inject.{Inject, Singleton}
import scalafx.application.Platform
import scalafx.beans.property.StringProperty
import scalafx.scene.control.{Label, Tooltip}
import scalafx.scene.layout.{GridPane, Pane, StackPane, VBox}

@Singleton
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

  def show(styledMessage: StyledMessage): Unit =
    Platform.runLater {
      grid.children.clear()

      val label = new Label(styledMessage.text) {
        styleClass += styledMessage.css
//        if styledMessage.css == "duplicate-qso" then
//          style = "-fx-text-fill: red; -fx-font-weight: bold;"
      }
      grid.add(label, 0, 0, 9, 1)
      root.visible = true
      root.managed = true
    }

  def clear: Unit =
    Platform.runLater {
      root.visible = false
      root.managed = false
      grid.children.clear()
    }

  def pane(): Pane =
    root = GridUtils.fieldSet("Duplicates", grid)
    root.visible = false
    root.managed = false
    root
