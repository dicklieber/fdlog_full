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

import com.organization.BuildInfo.version
import fdswarm.contestStart.ContestStartManager
import fdswarm.fx.contest.{ContestConfig, ContestConfigManager}
import fdswarm.fx.contest.ContestType.NONE
import fdswarm.fx.discovery.{ContestConfigDialog, ContestDiscovery}
import fdswarm.fx.utils.StyledDialog
import jakarta.inject.{Inject, Singleton}
import scalafx.Includes.*
import scalafx.geometry.Insets
import scalafx.scene.Node
import scalafx.scene.control.{Button, ButtonType, Label, OverrunStyle, Tooltip}
import scalafx.scene.layout.{BorderPane, GridPane, Region, StackPane}
import scalafx.stage.Window

@Singleton
final class WelcomeDialog @Inject() (
    contestConfigManager: ContestConfigManager,
    contestStartManager: ContestStartManager,
    contestDiscovery: ContestDiscovery,
    contestConfigDialog: ContestConfigDialog
):

  def show(
      ownerWindow: Window
  ): Unit =
    val dialog = new StyledDialog[ButtonType]:
      title = "Welcome"
      headerText = "Welcome to FdSwarm"
      initOwner(ownerWindow)
    val contentPane = new BorderPane:
      padding = Insets(10)

    def label(textValue: String): Label =
      new Label(textValue):
        minWidth = Region.USE_PREF_SIZE
        textOverrun = OverrunStyle.Clip

    def gridCell(node: Node, row: Int, column: Int, rowSpan: Int = 1, columnSpan: Int = 1): StackPane =
      val totalRows = 4
      val totalColumns = 3
      val right = if column + columnSpan == totalColumns then 1 else 0
      val bottom = if row + rowSpan == totalRows then 1 else 0
      new StackPane:
        padding = Insets(2, 4, 2, 4)
        minWidth = Region.USE_PREF_SIZE
        style = s"-fx-border-color: #666; -fx-border-width: 1 $right $bottom 1;"
        children = Seq(node)

    def rebuildGridPane(): GridPane =
      val contestConfig = contestConfigManager.contestConfigProperty.value
      val contestType =
        if contestConfig.contestType == NONE then "Not defined"
        else s"${contestConfig.ourCallsign} ${contestConfig.transmitters}${contestConfig.ourClass} ${contestConfig.ourSection}"
      val contestStarted = contestStartManager.contestStart.value.toString
      val contestLabel = label(contestType)
      contestLabel.tooltip = Tooltip(contestTooltipText(contestConfig))
      val setupContestButton = new Button("Setup"):
        minWidth = Region.USE_PREF_SIZE
        textOverrun = OverrunStyle.Clip
        onAction = _ =>
          contestConfigDialog.showAndWait()
          contentPane.center = rebuildGridPane()

      new GridPane:
        hgap = 0
        vgap = 0

        add(gridCell(label("FdSwarm"), 0, 0), 0, 0)
        add(gridCell(label(version), 0, 1), 1, 0)
        add(gridCell(label(""), 0, 2), 2, 0)

        add(gridCell(label("Contest"), 1, 0, rowSpan = 2), 0, 1, 1, 2)
        add(gridCell(label(contestConfig.contestType.toString), 1, 1), 1, 1)
        add(gridCell(setupContestButton, 1, 2, rowSpan = 2), 2, 1, 1, 2)

        add(gridCell(contestLabel, 2, 1), 1, 2)

        add(gridCell(label("contestStarted"), 3, 0), 0, 3)
        add(gridCell(label(contestStarted), 3, 1), 1, 3)
        add(gridCell(label(""), 3, 2), 2, 3)

    dialog.dialogPane().buttonTypes = Seq(ButtonType.OK)
    contentPane.center = rebuildGridPane()
    dialog.dialogPane().content = contentPane

    dialog.showAndWait()
  def isContestGoing: Boolean =
    contestConfigManager.contestType != NONE || contestStartManager.contestStart.value.isStarted

  private def contestTooltipText(contestConfig: ContestConfig): String =
    Seq(
      "Contest type" -> contestConfig.contestType.toString,
      "Callsign" -> Option(contestConfig.ourCallsign).map(_.toString).getOrElse(""),
      "Transmitters" -> contestConfig.transmitters.toString,
      "Class" -> contestConfig.ourClass,
      "Section" -> contestConfig.ourSection
    ).map { case (label, value) => s"$label: $value" }.mkString("\n")
