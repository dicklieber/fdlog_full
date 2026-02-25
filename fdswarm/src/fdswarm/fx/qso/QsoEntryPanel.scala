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
import fdswarm.StationManager
import fdswarm.fx.bandmodes.SelectedBandModeStore
import fdswarm.fx.contest.ContestManager
import fdswarm.fx.{CallSignField, GridUtils}
import fdswarm.model.*
import fdswarm.replication.MulticastTransport
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
                               multicastTransport: MulticastTransport,
                               selectedBandModeStore: SelectedBandModeStore,
                               stationManager: StationManager,
                               contestManager: ContestManager,
                               callsignField: CallSignField,
                               contestClassField: ContestClassField,
                               sectionField: fdswarm.fx.sections.SectionField,
                               dupPanel: DupPanel,
                               hostAndPortProvider: HostAndPortProvider
                             ) extends LazyLogging:

  lazy val node: Node =
    GridUtils.fieldSet("QSO", mainLayout)
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

  def sectionFieldProperty: scalafx.beans.property.StringProperty =
    sectionField.text

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
      Callsign(callsignField.text.value),
      contestClassField.text.value,
      sectionField.text.value,
      selectedBandModeStore.selected.value,
      qsoMetadata
    )

    val styledMessage: StyledMessage = qsoStore.add(qso)
    clearControls
    dupPanel.show(styledMessage)

  sectionField.onAction = _ => submit()

  private def qsoMetadata =
    QsoMetadata(
      station = stationManager.station,
      node = "local",
      contest = contestManager.config.contest
    )

  private def clearControls: Unit =
    callsignField.text = ""
    contestClassField.text = ""
    sectionField.text = ""
    callsignField.requestFocus()
