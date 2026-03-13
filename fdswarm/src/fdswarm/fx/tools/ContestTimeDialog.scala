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

import fdswarm.fx.contest.{ContestManager, ContestTimes}
import fdswarm.fx.qso.ContestTimerPanel
import jakarta.inject.{Inject, Singleton}
import scalafx.Includes.*
import scalafx.geometry.Insets
import scalafx.scene.Scene
import scalafx.scene.control.*
import scalafx.scene.layout.{GridPane, HBox, Region}
import scalafx.stage.{Stage, Window}
import java.time.{LocalTime, ZonedDateTime}

@Singleton
class ContestTimeDialog @Inject()(contestManager: ContestManager, contestTimerPanel: ContestTimerPanel) {

  private var stage: Option[Stage] = None

  def show(ownerWindow: Window): Unit = {
    stage match {
      case Some(s) => s.requestFocus()
      case None =>
        val newStage = new Stage {
          title = "Contest Time"
          initOwner(ownerWindow)
          resizable = false
        }

        val useMockTimeCheckBox = new CheckBox("Use Mock time") {
          selected = false
          minWidth = Region.USE_PREF_SIZE
        }

        val now = ZonedDateTime.now()
        val mockTimeEditor = new ZonedDateTimeEditor(now, "Mock Time")

        def updatePanel(): Unit = {
          contestTimerPanel.setMockTime(useMockTimeCheckBox.selected.value, mockTimeEditor.value)
        }

        useMockTimeCheckBox.onAction = _ => updatePanel()
        mockTimeEditor.setOnAction(updatePanel())

        val mockTimeBox = new HBox(5, mockTimeEditor)
        mockTimeBox.disable <== useMockTimeCheckBox.selected.not()

        // --- Contest Times ---
        val times = contestManager.contestTimesProperty.value
        val startEditor = new ZonedDateTimeEditor(times.start, "Contest Start")
        val endEditor = new ZonedDateTimeEditor(times.end, "Contest End")

        def updateContestTimes(): Unit = {
          contestManager.contestTimesProperty.value = ContestTimes(startEditor.value, endEditor.value)
        }

        startEditor.setOnAction(updateContestTimes())
        endEditor.setOnAction(updateContestTimes())

        val root = new GridPane {
          hgap = 10
          vgap = 10
          padding = Insets(10)
          add(useMockTimeCheckBox, 0, 0, 2, 1)
          add(new Label("Mock Time:") { minWidth = Region.USE_PREF_SIZE }, 0, 1)
          add(mockTimeBox, 1, 1)

          add(new Separator(), 0, 2, 2, 1)

          add(new Label("Contest Start:") { minWidth = Region.USE_PREF_SIZE }, 0, 3)
          add(startEditor, 1, 3)
          add(new Label("Contest End:") { minWidth = Region.USE_PREF_SIZE }, 0, 4)
          add(endEditor, 1, 4)
        }

        newStage.scene = new Scene(root)
        newStage.onCloseRequest = _ => stage = None
        newStage.show()
        stage = Some(newStage)
    }
  }
}
