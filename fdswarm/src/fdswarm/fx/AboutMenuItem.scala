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

package fdswarm.fx

import scalafx.scene.control.Alert
import scalafx.scene.control.Alert.AlertType
import scalafx.scene.control.MenuItem
import scalafx.stage.Window
import jakarta.inject.Inject

class AboutMenuItem @Inject() () extends MenuItem("About"):
  def showAboutDialog(window: Window): Unit =
    new Alert(AlertType.Information):
      initOwner(window)
      title = "About FDLog"
      headerText = "FDLog Swarm"
      contentText = s"Version: ${com.organization.BuildInfo.version}\n" +
        s"Scala: ${com.organization.BuildInfo.scalaVersion}\n" +
        "Copyright (c) 2026 Dick Lieber, WA9NNN"
    .showAndWait()

  def setOwner(window: Window): Unit =
    onAction = _ => showAboutDialog(window)
