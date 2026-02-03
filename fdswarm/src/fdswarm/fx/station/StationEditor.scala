package fdswarm.fx.station

import fdswarm.model.{Callsign, Station}
import jakarta.inject.{Inject, Singleton}
import scalafx.Includes.*
import scalafx.beans.binding.Bindings
import scalafx.geometry.Insets
import scalafx.scene.control.*
import scalafx.scene.layout.GridPane
import scalafx.stage.Window

import scala.util.Try

@Singleton
final class StationEditor @Inject() (stationStore: StationStore) {

  /** Called by FdLogUi */
  def show(ownerWindow: Window): Unit = {
    val initial = stationStore.station.value

    val rigField = new TextField {
      text = initial.rig
      promptText = "Rig (e.g. IC-7300)"
    }

    val antennaField = new TextField {
      text = initial.antenna
      promptText = "Antenna (e.g. Dipole)"
    }

    val operatorField = new TextField {
      text = initial.operator.value
      promptText = "Operator callsign (e.g. WA9NNN)"
    }

    // Force callsign to uppercase as user types (no recursion loop)
    operatorField.text.onChange { (_, _, newValue) =>
      val up = Option(newValue).getOrElse("").toUpperCase
      if up != newValue then operatorField.text = up
    }

    val dialog = new Dialog[ButtonType] {
      title = "Station"
      headerText = "Edit Station"
      initOwner(ownerWindow)
    }

    val saveBtnType = new ButtonType("Save", ButtonBar.ButtonData.OKDone)
    dialog.dialogPane().buttonTypes = Seq(ButtonType.Cancel, saveBtnType)

    dialog.dialogPane().content = new GridPane {
      hgap = 10
      vgap = 10
      padding = Insets(10)

      add(new Label("Rig:"), 0, 0)
      add(rigField, 1, 0)

      add(new Label("Antenna:"), 0, 1)
      add(antennaField, 1, 1)

      add(new Label("Operator:"), 0, 2)
      add(operatorField, 1, 2)
    }

    // If Callsign ever validates/throws, this disables save.
    def canBuildStation: Boolean =
      Try(Callsign(operatorField.text.value.trim)).isSuccess

    val saveBtnNode = dialog.dialogPane().lookupButton(saveBtnType)
    saveBtnNode.disable <== Bindings.createBooleanBinding(
      () => !canBuildStation,
      operatorField.text
    )

    dialog.resultConverter = (btn: ButtonType) => btn

    val opt = dialog.delegate.showAndWait()
    if opt.isPresent && opt.get == saveBtnType then {
      val newStation = Station(
        rig = rigField.text.value.trim,
        antenna = antennaField.text.value.trim,
        operator = Callsign(operatorField.text.value.trim)
      )
      stationStore.update(newStation)
    }
  }
}