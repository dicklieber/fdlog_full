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

import fdswarm.logging.LazyStructuredLogging
import fdswarm.StationConfigManager
import fdswarm.fx.bandmodes.SelectedBandModeManager
import fdswarm.fx.contest.{ContestConfigManager, ContestType}
import fdswarm.fx.sections.Section
import fdswarm.fx.CallSignField
import fdswarm.fx.GridColumns
import fdswarm.model.*
import fdswarm.replication.{Service, Transport}
import fdswarm.store.{QsoStore, StyledMessage}
import fdswarm.util.*
import jakarta.inject.{Inject, Singleton}
import scalafx.Includes.*
import scalafx.application.Platform
import scalafx.scene.Node
import scalafx.scene.control.*
import scalafx.scene.layout.{GridPane, VBox}

/**
 * A user interface panel for handling QSO (contact) entry in an amateur radio logging application.
 *
 * This panel facilitates the input, validation, and submission of QSO data, including the callsign,
 * contest class, and received section. It manages user interactions and integrates necessary services
 * for dupe checking, metadata handling, and QSO storage.
 *
 * Dependencies:
 * - `QsoStore`: Handles storage and retrieval of QSO data.
 * - `Transport`: Facilitates communication or messaging, if applicable.
 * - `SelectedBandModeManager`: Manages the selected band and mode for operation.
 * - `StationConfigManager`: Provides station configuration details.
 * - `ContestConfigManager`: Manages configuration data for the current contest.
 * - `CallSignField`: Input field for the loggable callsign.
 * - `ContestClassField`: Input field for the contest class.
 * - `sectionField`: Input field for the received section.
 * - `DupPanel`: Displays potential duplicate QSO information.
 * - `NodeIdentityManager`: Manages node identity for distributed logging configurations.
 *
 * Features:
 * - Builds the UI dynamically and ensures it is initialized only once.
 * - Provides live validation and entry property bindings for input fields.
 * - Integrates with QSO storage for checking duplicate entries based on callsign and band/mode.
 * - Automatically handles focus traversal between fields during data entry.
 * - Clears fields and resets focus after QSO submission.
 * - Supports metadata management, including contest type, station, and node identity.
 *
 * Primary Methods:
 * - `buildUi`: Builds the user interface if it hasn't already been initialized.
 * - `node`: Returns the main visual node representing the panel.
 * - `callsignValidProperty`: Exposes a boolean property indicating the validity of the callsign field.
 * - `contestClassValidProperty`: Exposes a boolean property indicating the validity of the contest class field.
 * - `sectionFieldProperty`: Provides a property for accessing/modifying the section field text.
 * - `sectionFieldFocusedProperty`: Indicates whether the section field currently has focus.
 * - `submit`: Compiles and submits a QSO entry based on the input data, clears fields, and updates the dupe panel.
 *
 * Internal Behavior:
 * - Input fields trigger event-driven validation and focus shifting to facilitate smooth data entry.
 * - Duplicate detection is performed dynamically based on callsign input.
 * - Clears all input controls upon submission to prepare for the next entry.
 */
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
                             ) extends LazyStructuredLogging:

  private val _node = new VBox()
  def node: Node = _node

  private var uiBuilt = false

  def buildUi(): Unit =
    if uiBuilt then return
    _node.children = Seq(GridColumns.fieldSet("QSO", mainLayout))
    uiBuilt = true

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
  private val mainLayout = new VBox {
    spacing = 10
    children = Seq(
      grid,
      dupPanel.pane()
    )
  }
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

      contestClassField.requestFocus()
      contestClassField.end()
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
    val contestType: ContestType = contestManager.contestConfigProperty.value.contestType
    QsoMetadata(
      station = stationManager.station,
      node = nodeIdentityManager.ourNodeIdentity,
      contest = contestType
    )

  private def clearControls: Unit =
    callsignField.text = ""
    contestClassField.text = ""
    sectionField.text = ""
    callsignField.requestFocus()
