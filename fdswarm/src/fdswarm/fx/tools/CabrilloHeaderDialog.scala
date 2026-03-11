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

import fdswarm.fx.InputHelper.forceCaps
import fdswarm.exporter.*
import fdswarm.fx.contest.ContestManager
import fdswarm.fx.station.StationStore
import fdswarm.fx.utils.{BootstrapIcons, IconButton}
import jakarta.inject.{Inject, Singleton}
import scalafx.Includes.*
import scalafx.geometry.{Insets, Pos}
import scalafx.scene.control.*
import scalafx.scene.layout.{GridPane, Priority, Region, VBox}
import scalafx.stage.Window

@Singleton
final class CabrilloHeaderDialog @Inject()(
                                            headerStore: CabrilloHeaderStore,
                                            stationStore: StationStore,
                                            contestManager: ContestManager
                                          ):

  def show(ownerWindow: Window): Unit =
    val currentHeader = headerStore.header.value
    val station = stationStore.station.value
    val contest = contestManager.config

    val callsignField = new TextField {
      text = contest.ourCallsign.toString
      editable = false
    }
    val contestField = new TextField {
      text = contest.contest.toString
      editable = false
    }
    
    val categoryOperatorCombo = new ComboBox[CategoryOperator](CategoryOperator.values.toIndexedSeq) { value = currentHeader.categoryOperator }
    val categoryAssistedCombo = new ComboBox[CategoryAssisted](CategoryAssisted.values.toIndexedSeq) { value = currentHeader.categoryAssisted }
    val categoryBandCombo = new ComboBox[CategoryBand](CategoryBand.values.toIndexedSeq) { value = currentHeader.categoryBand }
    val categoryModeCombo = new ComboBox[CategoryMode](CategoryMode.values.toIndexedSeq) { value = currentHeader.categoryMode }
    val categoryPowerCombo = new ComboBox[CategoryPower](CategoryPower.values.toIndexedSeq) { value = currentHeader.categoryPower }
    val categoryStationCombo = new ComboBox[CategoryStation](CategoryStation.values.toIndexedSeq) { value = currentHeader.categoryStation }
    val categoryTransmitterCombo = new ComboBox[CategoryTransmitter](CategoryTransmitter.values.toIndexedSeq) { value = currentHeader.categoryTransmitter }
    val categoryOverlayCombo = new ComboBox[CategoryOverlay](CategoryOverlay.values.toIndexedSeq) { value = currentHeader.categoryOverlay }

    val clubField = new TextField { text = currentHeader.club }
    val operatorsField = new TextField { text = currentHeader.operators }
    forceCaps(operatorsField)
    val nameField = new TextField { text = currentHeader.name }
    val addressField = new TextField { text = currentHeader.address }
    val cityField = new TextField { text = currentHeader.addressCity }
    val stateField = new TextField { text = currentHeader.addressStateProvince }
    val postalCodeField = new TextField { text = currentHeader.addressPostalCode }
    val countryField = new TextField { text = currentHeader.addressCountry }
    val stationClassField = new TextField {
      text = s"${contest.transmitters}${contest.ourClass}"
      editable = false
    }
    val stationSectionField = new TextField {
      text = contest.ourSection
      editable = false
    }
    val soapboxField = new TextArea { 
      text = currentHeader.soapbox
      prefRowCount = 3
    }

    val dialog = new Dialog[ButtonType]:
      initOwner(ownerWindow)
      title = "Cabrillo Header Configuration"
      headerText = "Configure Cabrillo Header Fields"
      resizable = true

    val helpButtonType = new ButtonType("", ButtonBar.ButtonData.Help)
    val saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OKDone)
    dialog.dialogPane().buttonTypes = Seq(helpButtonType, ButtonType.Cancel, saveButtonType)
    dialog.dialogPane().setPrefWidth(600)

    val helpButton = dialog.dialogPane().lookupButton(helpButtonType).asInstanceOf[javafx.scene.control.Button]
    helpButton.setGraphic(BootstrapIcons.svgPath("info-circle-fill", 24).delegate)
    helpButton.setTooltip(new javafx.scene.control.Tooltip("Cabrillo Specification (WWROF)"))
    helpButton.setStyle("-fx-background-color: transparent; -fx-border-color: transparent; -fx-padding: 6;")
    helpButton.setOnAction(_ =>
      java.awt.Desktop.getDesktop.browse(new java.net.URI("https://wwrof.org/cabrillo/cabrillo-v3-header/"))
    )

    val grid = new GridPane:
      hgap = 10
      vgap = 10
      padding = Insets(20)

      var row = 0
      def addRow(label: String, control: Control, tooltipText: String): Unit =
        val l = new Label(label) {
          minWidth = Region.USE_PREF_SIZE
          tooltip = new Tooltip(tooltipText)
        }
        control.tooltip = new Tooltip(tooltipText)
        add(l, 0, row)
        add(control, 1, row)
        GridPane.setHgrow(control, Priority.Always)
        row += 1

      addRow("Callsign (from Contest Config):", callsignField, "The callsign used during the contest.")
      addRow("Contest (from Contest Config):", contestField, "String to identify the contest (e.g., ARRL-FIELD-DAY).")
      addRow("Category Operator:", categoryOperatorCombo, "The category-operator must be one of: SINGLE-OP, MULTI-OP, CHECKLOG.")
      addRow("Category Assisted:", categoryAssistedCombo, "The category-assisted must be one of: ASSISTED, NON-ASSISTED.")
      addRow("Category Band:", categoryBandCombo, "The category-band must be ALL or one of the individual bands (e.g., 160M, 80M, etc.).")
      addRow("Category Mode:", categoryModeCombo, "The category-mode must be one of: CW, DIGI, FM, RTTY, SSB, MIXED.")
      addRow("Category Power:", categoryPowerCombo, "The category-power must be one of: HIGH, LOW, QRP.")
      addRow("Category Station:", categoryStationCombo, "Type of station (e.g., FIXED, MOBILE, PORTABLE, ROVER, etc.).")
      addRow("Category Transmitter:", categoryTransmitterCombo, "The category-transmitter is required for multi-operator entries (e.g., ONE, TWO, LIMITED, UNLIMITED, etc.).")
      addRow("Category Overlay:", categoryOverlayCombo, "Category overlay (e.g., CLASSIC, ROOKIE, TB-WIRES, etc.).")
      addRow("Club:", clubField, "Name of the radio club to which the score should be applied.")
      addRow("Operators (comma separated):", operatorsField, "A space or comma-delimited list of operator callsign(s).")
      addRow("Name:", nameField, "Contact name for the entry (max 75 characters).")
      addRow("Address:", addressField, "Mailing address (max 45 characters per line).")
      addRow("City:", cityField, "Mailing address city.")
      addRow("State/Province:", stateField, "Mailing address state or province.")
      addRow("Postal Code:", postalCodeField, "Mailing address postal code.")
      addRow("Country:", countryField, "Mailing address country.")
      addRow("Station Class (from Contest Config):", stationClassField, "The ARRL section or LOCATION where the station was operating from.")
      addRow("Station Section (from Contest Config):", stationSectionField, "The ARRL section or LOCATION where the station was operating from.")
      addRow("Soapbox:", soapboxField, "Soapbox comments (max 75 characters per line).")

    val scrollPane = new ScrollPane:
      content = grid
      fitToWidth = true
      prefHeight = 500

    dialog.dialogPane().content = scrollPane
    dialog.resultConverter = (btn: ButtonType) => btn

    val result = dialog.showAndWait()
    if result.contains(saveButtonType) then
      val newHeader = CabrilloHeader(
        callsign = contest.ourCallsign.toString,
        contest = contest.contest.toString,
        categoryOperator = categoryOperatorCombo.value.value,
        categoryAssisted = categoryAssistedCombo.value.value,
        categoryBand = categoryBandCombo.value.value,
        categoryMode = categoryModeCombo.value.value,
        categoryPower = categoryPowerCombo.value.value,
        categoryStation = categoryStationCombo.value.value,
        categoryTransmitter = categoryTransmitterCombo.value.value,
        categoryOverlay = categoryOverlayCombo.value.value,
        club = clubField.text.value.trim,
        operators = operatorsField.text.value.trim,
        name = nameField.text.value.trim,
        address = addressField.text.value.trim,
        addressCity = cityField.text.value.trim,
        addressStateProvince = stateField.text.value.trim,
        addressPostalCode = postalCodeField.text.value.trim,
        addressCountry = countryField.text.value.trim,
        stationClass = s"${contest.transmitters}${contest.ourClass}",
        stationSection = contest.ourSection,
        soapbox = soapboxField.text.value.trim
      )
      headerStore.update(newHeader)
