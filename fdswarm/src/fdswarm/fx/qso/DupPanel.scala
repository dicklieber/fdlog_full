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
  private var titleLabel: Label = _

  def show(styledMessage: StyledMessage): Unit =
    Platform.runLater {
      titleLabel.text = "Saving Qso"
      grid.children.clear()

      val label = new Label(styledMessage.text) {
        styleClass += styledMessage.css
      }
      grid.add(label, 0, 0, 9, 1)
      root.visible = true
      root.managed = true
    }

  def show(dupInfo: DupInfo): Unit =
    Platform.runLater {
      titleLabel.text = "Potential Duplicates"
      grid.children.clear()
      if (dupInfo.totalDups > 0) {
        dupInfo.firstNDups.zipWithIndex.foreach { (callsign, index) =>
          val callLabel = new Label(callsign.value) {
            styleClass += "dup-callsign"
          }
          grid.add(callLabel, index % 7, index / 7)
        }
        val moreCount = dupInfo.totalDups - dupInfo.firstNDups.size
        if (moreCount > 0) {
          val moreLabel = new Label(s"$moreCount more potential duplicates.") {
            styleClass += "dupCallsignMore"
          }
          val row = ((dupInfo.firstNDups.size - 1) / 7) + 1
          grid.add(moreLabel, 0, row, 7, 1)
        }
        root.visible = true
        root.managed = true
      } else {
        root.visible = false
        root.managed = false
      }
    }

  /**
   * Displays a help section with a title and corresponding items.
   *
   * @param title The title of the help section to be displayed.
   * @param items A sequence of pairs where each pair contains a code (String) 
   *              and its corresponding description (String) to be displayed 
   *              in the help section.
   * @return Unit This method does not return a value. It updates the UI to 
   *         show the help section.
   */
  def show(title: String, items: Seq[(String, String)]): Unit =
    Platform.runLater {
      titleLabel.text = title
      grid.children.clear()
      items.zipWithIndex.foreach { case ((code, desc), index) =>
        val codeLabel = new Label(code) {
          styleClass += "help-code"
          style = "-fx-font-weight: bold;"
        }
        val descLabel = new Label(desc) {
          styleClass += "help-desc"
        }
        grid.add(codeLabel, 0, index)
        grid.add(descLabel, 1, index)
      }
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
    val stackPane = GridUtils.fieldSet("XYZZY", grid)
    titleLabel = new Label(stackPane.children(1).asInstanceOf[javafx.scene.control.Label])
    root = stackPane
    root.visible = false
    root.managed = false
    root
