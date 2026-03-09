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

import fdswarm.replication.StatusBroadcastService
import jakarta.inject.{Inject, Singleton}
import scalafx.Includes.*
import scalafx.geometry.{Insets, Pos}
import scalafx.scene.control.*
import scalafx.scene.layout.{HBox, VBox}
import scalafx.stage.Window

@Singleton
final class StatusBroadcastDialog @Inject() (
                                              statusBroadcastService: StatusBroadcastService
                                            ) {

  def show(ownerWindow: Window): Unit = {
    val periodicCheckbox = new CheckBox("Enable periodic Status broadcasts") {
      selected <==> statusBroadcastService.periodicEnabledProperty
    }

    val broadcastButton = new Button("Broadcast Status Now") {
      onAction = _ => statusBroadcastService.broadcastStatus()
      disable <== periodicCheckbox.selected
    }

    val periodSpinner = new Spinner[Int](1, 3600, statusBroadcastService.broadcastPeriodSecProperty.value) {
      editable = true
      prefWidth = 80
    }
    periodSpinner.valueFactory.value.valueProperty().addListener { (_, _, newValue) =>
      statusBroadcastService.broadcastPeriodSecProperty.value = newValue.intValue()
    }
    statusBroadcastService.broadcastPeriodSecProperty.onChange { (_, _, newValue) =>
      periodSpinner.valueFactory.value.value = newValue.intValue()
    }

    val transportToggleGroup = new ToggleGroup()
    val multicastRadio = new RadioButton("Multicast") {
      toggleGroup = transportToggleGroup
      userData = "Multicast"
    }
    val broadcastRadio = new RadioButton("Broadcast") {
      toggleGroup = transportToggleGroup
      userData = "Broadcast"
    }

    if (statusBroadcastService.transportTypeProperty.value == "Broadcast") {
      broadcastRadio.selected = true
    } else {
      multicastRadio.selected = true
    }

    transportToggleGroup.selectedToggle.onChange { (_, _, newToggle) =>
      if (newToggle != null) {
        statusBroadcastService.transportTypeProperty.value = newToggle.asInstanceOf[javafx.scene.control.RadioButton].getUserData.toString
      }
    }

    val resetButton = new Button("Reset") {
      onAction = _ => {
        periodSpinner.valueFactory.value.value = statusBroadcastService.defaultBroadcastPeriodSec
      }
      tooltip = Tooltip("Reset to default from application.conf")
    }

    val dialog = new Dialog[Unit] {
      title = "Status Broadcast Settings"
      headerText = "Configure Status broadcasts"
      initOwner(ownerWindow)
    }

    dialog.dialogPane().buttonTypes = Seq(ButtonType.Close)

    dialog.dialogPane().content = new VBox {
      spacing = 15
      padding = Insets(20)
      children = Seq(
        periodicCheckbox,
        new HBox {
          spacing = 10
          alignment = Pos.CenterLeft
          children = Seq(
            new Label("Broadcast Period (sec):"),
            periodSpinner,
            resetButton
          )
        },
        new HBox {
          spacing = 10
          alignment = Pos.CenterLeft
          children = Seq(
            new Label("Transport:"),
            multicastRadio,
            broadcastRadio
          )
        },
        broadcastButton
      )
    }

    dialog.showAndWait()
  }
}
