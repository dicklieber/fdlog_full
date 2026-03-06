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

import fdswarm.replication.SwarmStatusPane
import jakarta.inject.{Inject, Singleton}
import scalafx.Includes.*
import scalafx.scene.Scene
import scalafx.scene.layout.StackPane
import scalafx.stage.Stage
import scalafx.stage.Window

@Singleton
class SwarmStatusAdmin @Inject()(swarmStatusPane: SwarmStatusPane):

  private var stage: Option[Stage] = None

  def show(ownerWindow: Window): Unit =
    stage match
      case Some(s) =>
        s.toFront()
      case None =>
        val s = new Stage():
          initOwner(ownerWindow)
          title = "Swarm Status"
          scene = new Scene:
            root = swarmStatusPane.node
            stylesheets = Seq(getClass.getResource("/styles/app.css").toExternalForm)

        s.onCloseRequest = _ =>
          s.scene().root = new StackPane() // Detach swarmStatusPane.node
          stage = None
        s.show()
        stage = Some(s)
