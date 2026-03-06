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

import fdswarm.fx.qso.ContestTimerPanel
import jakarta.inject.{Inject, Singleton}
import fdswarm.fx.SfxUtils.*
import scalafx.geometry.Insets
import scalafx.scene.Scene
import scalafx.scene.control.*
import scalafx.scene.layout.{GridPane, HBox}
import scalafx.stage.{Stage, Window}
import java.time.{LocalTime, ZonedDateTime}

@Singleton
class ContestTimeDialog @Inject()(contestTimerPanel: ContestTimerPanel) {

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

        val useFixedTimeCheckBox = new CheckBox("Use Fixed time") {
          selected = false
        }

        val now = ZonedDateTime.now()
        val datePicker = new DatePicker(now.toLocalDate)
        val hourSpinner = new Spinner[Int](0, 23, now.getHour) { prefWidth = 60 }
        val minSpinner = new Spinner[Int](0, 59, now.getMinute) { prefWidth = 60 }

        def currentSelectedTime: ZonedDateTime =
          ZonedDateTime.of(datePicker.value.value, LocalTime.of(hourSpinner.value.value, minSpinner.value.value), now.getZone)

        def updatePanel(): Unit = {
          contestTimerPanel.setFixedTime(useFixedTimeCheckBox.selected.value, currentSelectedTime)
        }

        useFixedTimeCheckBox.onAction = _ => updatePanel()
        datePicker.onAction = _ => updatePanel()
        hourSpinner.value.onChange { (_, _, _) => updatePanel() }
        minSpinner.value.onChange { (_, _, _) => updatePanel() }

        val timeBox = new HBox(5, datePicker, new Label("H:"), hourSpinner, new Label("M:"), minSpinner)
        timeBox.disable <== useFixedTimeCheckBox.selected.not()

        val root = new GridPane {
          hgap = 10
          vgap = 10
          padding = Insets(10)
          add(useFixedTimeCheckBox, 0, 0, 2, 1)
          add(new Label("Fixed Time:"), 0, 1)
          add(timeBox, 1, 1)
        }

        newStage.scene = new Scene(root)
        newStage.onCloseRequest = _ => stage = None
        newStage.show()
        stage = Some(newStage)
    }
  }
}
