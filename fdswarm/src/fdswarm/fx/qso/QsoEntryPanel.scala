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

package fdswarm.fx.qso

import com.typesafe.scalalogging.LazyLogging
import fdswarm.StationConfigManager
import fdswarm.fx.bandmodes.SelectedBandModeManager
import fdswarm.fx.contest.ContestConfigManager
import fdswarm.fx.sections.Section
import fdswarm.fx.CallSignField
import fdswarm.fx.GridColumns
import fdswarm.model.*
import fdswarm.replication.{Service, Transport}
import fdswarm.store.{QsoStore, StyledMessage}
import fdswarm.util.*
import jakarta.inject.{Inject, Singleton}
import scalafx.application.Platform
import scalafx.scene.Node
import scalafx.scene.control.*
import scalafx.scene.layout.{GridPane, VBox}

@Singleton
class QsoEntryPanel @Inject()(
                               qsoStore: QsoStore,
                               transport: Transport,
                               selectedBandModeStore: SelectedBandModeManager,
                               stationManager: StationConfigManager,
                               contestManager: ContestConfigManager,
                               callsignField: CallSignField,
                               contestClassField: ContestClassField,
                               sectionField: fdswarm.fx.sections.SectionField,
                               dupPanel: DupPanel,
                               nodeIdentityManager: NodeIdentityManager
                             ) extends LazyLogging:

  lazy val node: Node =
    GridColumns.fieldSet("QSO", mainLayout)
  private val clearButton = new Button("\u21BA"):
    styleClass += "clear-button"
    tooltip = Tooltip("Clear fields")
    onAction.set(
      new javafx.event.EventHandler[javafx.event.ActionEvent]:
        override def handle(event: javafx.event.ActionEvent): Unit =
          callsignField.text = ""
          contestClassField.text = ""
          sectionField.text = ""
          callsignField.requestFocus()
        end handle
    )
  private val grid = new GridPane:
    hgap = 5
    add(new Label("Their Callsign:"), 0, 0)
    add(callsignField, 0, 1)

    add(new Label("Received Class:"), 1, 0)
    add(contestClassField, 1, 1)

    add(new Label("Received Section:"), 2, 0)
    add(sectionField, 2, 1)

    add(clearButton, 3, 1)
  private val mainLayout = new VBox:
    spacing = 10
    children = Seq(
      grid,
      dupPanel.pane()
    )

  def callsignValidProperty: scalafx.beans.property.BooleanProperty =
    callsignField.validProperty

  def contestClassValidProperty: scalafx.beans.property.BooleanProperty =
    contestClassField.validProperty

  def sectionFieldProperty: scalafx.beans.property.StringProperty =
    sectionField.text

  def sectionFieldFocusedProperty: scalafx.beans.property.ReadOnlyBooleanProperty =
    sectionField.focused

  callsignField.text.onChange { (_, _, newValue) =>
    if newValue.length < 3 then dupPanel.clear
    else
      val dupInfo =
        qsoStore.potentialDups(newValue, selectedBandModeStore.selected.value)
      dupPanel.show(dupInfo)
  }

  callsignField.onDoneFunction = chForNext =>
    Platform.runLater {
      contestClassField.text = if chForNext.trim.isEmpty then "" else chForNext
      logger.debug("new class field: {}", contestClassField.text.value)

      contestClassField.requestFocus()
      contestClassField.end()
      logger.debug("new class field2: {}", contestClassField.text.value)
    }
  contestClassField.onDoneFunction = chForNext =>
    Platform.runLater {
      sectionField.text = if chForNext.trim.isEmpty then "" else chForNext

      sectionField.requestFocus()
      sectionField.end()
    }

  sectionField.onDoneFunction = _ =>
    Platform.runLater {
      submit()
    }

  def submit(): Unit =
    val qso = Qso(
      callsign = Callsign(callsignField.text.value),
      exchange = Exchange(FdClass(contestClassField.text.value), sectionField.text.value),
      bandMode = selectedBandModeStore.selected.value,
      qsoMetadata = qsoMetadata
    )

    val styledMessage: StyledMessage = qsoStore.add(qso)
    clearControls
    dupPanel.show(styledMessage)

  sectionField.onAction = _ => submit()

  private def qsoMetadata =
    QsoMetadata(
      station = stationManager.station,
      node = nodeIdentityManager.ourNodeIdentity,
      contest = contestManager.contestConfigProperty.contestType
    )

  private def clearControls: Unit =
    callsignField.text = ""
    contestClassField.text = ""
    sectionField.text = ""
    callsignField.requestFocus()
