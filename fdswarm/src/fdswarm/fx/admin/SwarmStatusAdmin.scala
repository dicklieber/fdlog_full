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

import fdswarm.replication.status.SwarmStatusPane
import jakarta.inject.{Inject, Singleton}
import scalafx.Includes.*
import scalafx.scene.control.{ButtonBar, ButtonType, Dialog}
import scalafx.scene.layout.BorderPane
import scalafx.stage.Window

@Singleton
class SwarmStatusAdmin @Inject()(swarmStatusPane: SwarmStatusPane):

  def show(ownerWindow: Window): Unit =
    val dialog = new Dialog[Unit]() {
      initOwner(ownerWindow)
      title = "Swarm Status"
    }

    val dialogPane = dialog.dialogPane.value
    dialogPane.stylesheets = Seq(getClass.getResource("/styles/app.css").toExternalForm)
    dialogPane.content = swarmStatusPane.node
    val clearButtonType = new ButtonType("Clear All Data", ButtonBar.ButtonData.Help2)
    dialogPane.buttonTypes = Seq(clearButtonType, ButtonType.Close)

    val clearButton = dialogPane.lookupButton(clearButtonType).asInstanceOf[javafx.scene.control.Button]
    clearButton.onAction = _ => swarmStatusPane.clearData()

    // Detach node when dialog is closed to allow it to be reused elsewhere if needed,
    // though DialogPane usually handles its content. In the previous Stage implementation,
    // it was explicitly detached.
    dialog.showAndWait()
    dialogPane.content = new BorderPane() // Detach swarmStatusPane.node
