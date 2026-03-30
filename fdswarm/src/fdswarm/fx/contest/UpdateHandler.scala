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

package fdswarm.fx.contest

import fdswarm.store.QsoStore
import jakarta.inject.Inject
import scalafx.scene.control.Alert.AlertType
import scalafx.scene.control.{Alert, ButtonType}

class UpdateHandlerobject @Inject() (
    qsoStore: QsoStore
):
  /**
   * Asks the user if he wants to continue with the update.
   * @return
   */
  def apply(): Boolean =
    var continue = true
    if qsoStore.hasQsos then
      new Alert(AlertType.Error):
        title = "Update Contest Configuration"
        headerText = "You already have QSOs logged!"
        buttonTypes = Seq(ButtonType.OK, ButtonType.Cancel)
        contentText =
          """You already have QSOs logged!
            |Changing contest configuration during the contest is bad.
            |Are you sure you want to continue?
            |""".stripMargin
        .showAndWait() match
        case Some(ButtonType.OK) =>
          //          val updatedContestConfig = contestConfigPane.finish()
          //          contestManager.setConfig(updatedContestConfig)
          continue = true
        case _ =>
          continue = false

    if continue then
      continue = new Alert(AlertType.Confirmation):
        headerText = "Start Contest"
        contentText =
          """This will remove all QSOs and use the new Contest Configuration.
            |All nodes in the swarm will be restarted. i.e remove QSOs and use new configuration.
            |Are you sure you want to continue?
            |""".stripMargin
        .showAndWait() match
        case Some(ButtonType.OK) =>
//          continue = true
        case _ =>
          continue = false
    continue
