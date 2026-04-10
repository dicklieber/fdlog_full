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
    val broadcastButton = new Button("Broadcast Status Now") {
      onAction = _ => statusBroadcastService.broadcastStatus()
    }

    val dialog = new Dialog[Unit] {
      title = "Status Broadcast"
      headerText = "Send a Status broadcast to peers"
      initOwner(ownerWindow)
    }

    dialog.dialogPane().buttonTypes = Seq(ButtonType.Close)

    dialog.dialogPane().content = new VBox {
      spacing = 12
      padding = Insets(20)
      children = Seq(
        new HBox {
          spacing = 12
          alignment = Pos.CenterLeft
          children = Seq(
            new Label("Send an immediate Status broadcast."),
            broadcastButton
          )
        }
      )
    }

    dialog.showAndWait()
  }
}
