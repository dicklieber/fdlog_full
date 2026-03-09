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
import fdswarm.fx.GridColumns
import fdswarm.store.{DupInfo, StyledMessage}
import jakarta.inject.Inject
import scalafx.application.Platform
import scalafx.scene.control.Label
import scalafx.scene.layout.{GridPane, VBox}

/**
 * Displays informaion while entering a Qso.
 */
class QsoInfoPanel @Inject()() extends VBox with LazyLogging:

  private val grid = new GridPane {
    hgap = 10
    vgap = 5
  }

  managed = false
  visible = false
  children = Seq(GridColumns.fieldSet("Possible Dups", grid))

  /**
   * After attepting to add a QSO, show an error message if the callsign is already in the database. Or success.
   * @param msg
   */
  def showMessage(msg: StyledMessage): Unit =
    Platform.runLater {
      grid.children.clear()

      val label = new Label(msg.text) {
        styleClass += msg.css
      }
      grid.add(label, 0, 0, 9, 1)
      visible = true
      managed = true
    }

  def showPotentialDups(dupInfo: DupInfo): Unit =
    Platform.runLater {
      if !dupInfo.hasPotentialDups then
        visible = false
        managed = false
      else
        grid.children.clear()
        val displayed = dupInfo.firstNDups
        displayed.zipWithIndex.foreach { (cs, index) =>
          val label = new Label(cs.toString) {
            styleClass += "dupCallsign"
          }
          grid.add(label, index % 9, index / 9)
        }
        if dupInfo.totalDups > displayed.size then
          val moreLabel = new Label(s"${dupInfo.totalDups - displayed.size} more possible dups") {
            styleClass += "dupCallsignMore"
          }
          // Place in the next row, spanning all columns
          val nextRow = (displayed.size + 8) / 9
          grid.add(moreLabel, 0, nextRow, 9, 1)
        visible = true
        managed = true
    }
