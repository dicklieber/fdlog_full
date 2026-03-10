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

import fdswarm.replication.status.SwarmStatusPane
import jakarta.inject.{Inject, Singleton}
import scalafx.Includes.*
import scalafx.geometry.{Insets, Pos}
import scalafx.scene.Scene
import scalafx.scene.control.{Button, Label, TextField}
import scalafx.scene.layout.{HBox, Priority, Region, StackPane, VBox}
import scalafx.stage.{Stage, Window}

@Singleton
class ScalaFxTestDialog @Inject()(swarmStatusPane: SwarmStatusPane) {

  private var stage: Option[Stage] = None

  def show(ownerWindow: Window): Unit = {
    stage match {
      case Some(s) => s.requestFocus()
      case None =>
        val inputField = new TextField {
          hgrow = Priority.Always
        }

        val statusLabel = new Label {
          text = "Waiting for input..."
        }

        val swarmStatusContainer = new StackPane {
          vgrow = Priority.Always
          children = Seq(swarmStatusPane.node)
        }

        val root = new VBox {
          prefHeight = 380.0
          prefWidth = 458.0
          spacing = 10
          padding = Insets(20)
          children = Seq(
            new Label {
              text = "ScalaFX Test Dialog"
              style = "-fx-font-size: 18px; -fx-font-weight: bold;"
            },
            new HBox {
              alignment = Pos.CenterLeft
              spacing = 10
              children = Seq(
                new Label("Enter some text:"),
                inputField
              )
            },
            new Button {
              text = "Click Me!"
              maxWidth = Double.MaxValue
              onAction = _ => {
                val text = inputField.text.value
                statusLabel.text = s"You entered: $text"
              }
            },
            statusLabel,
            swarmStatusContainer,
            new Region {
              vgrow = Priority.Always
            },
            new Button {
              text = "Close"
              maxWidth = Double.MaxValue
              onAction = _ => {
                stage.foreach(_.close())
              }
            }
          )
        }

        val newStage = new Stage {
          title = "ScalaFX Test Dialog"
          initOwner(ownerWindow)
          resizable = true
          scene = new Scene(root)
        }

        newStage.onCloseRequest = _ => stage = None
        newStage.show()
        stage = Some(newStage)
    }
  }
}
