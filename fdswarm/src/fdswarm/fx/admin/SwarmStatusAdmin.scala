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

package fdswarm.fx.admin

import fdswarm.replication.status.{NodeDataField, SwarmData}
import jakarta.inject.{Inject, Singleton}
import scalafx.Includes.*
import scalafx.beans.binding.Bindings
import scalafx.scene.Scene
import scalafx.scene.control.Label
import scalafx.scene.layout.VBox
import scalafx.stage.{Stage, Window}

@Singleton
class SwarmStatusAdmin @Inject()(swarmData: SwarmData):

  private var stage: Option[Stage] = None

  def show(ownerWindow: Window): Unit =
    stage match
      case Some(s) =>
        s.toFront()
      case None =>
        val swarmSizeLabel = new Label:
          text <== Bindings.createStringBinding(
            () => s"swarmdata.size: ${swarmData.size.value}",
            swarmData.size
          )

        val newStage = new Stage {
          initOwner(ownerWindow)
          title = "Swarm Status"
          scene = new Scene(
            new VBox:
              spacing = 8
              children = Seq(
                swarmSizeLabel,
                swarmData.buildGridPane(
                  NodeDataField.staticFields
                )
              )
          ) {
            stylesheets = Seq(getClass.getResource("/styles/app.css").toExternalForm)
          }
          sizeToScene()
        }
        newStage.onCloseRequest = _ =>
          newStage.scene.value.root = new javafx.scene.layout.Region() // Detach nodeData grid
          stage = None
        newStage.show()
        stage = Some(newStage)
