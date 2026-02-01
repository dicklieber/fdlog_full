package fdswarm.fx.bands

import fdswarm.model.BandMode.Band
import jakarta.inject.{Inject, Singleton}
import scalafx.Includes.*
import scalafx.geometry.Insets
import scalafx.scene.Node
import scalafx.scene.control.{CheckBox, ScrollPane, Tooltip}
import scalafx.scene.layout.{HBox, VBox}

@Singleton
final class BandCheckBoxPane @Inject()(
                                        availableBandsManager: AvailableBandsManager,
                                        hamBandCatalog: BandCatalog
                                      ) extends HBox:

  private val spacingPx = 6.0

  // Build the checkboxes first (no self-reference while constructing)
  private val checkBoxes: Seq[CheckBox] =
    hamBandCatalog.hamBands.map { band =>
      val cb = new CheckBox(band.bandName)

      cb.tooltip = new Tooltip(
        s"${band.bandClass}  ${band.startFrequencyHz}–${band.endFrequencyHz} Hz"
      )

      cb
    }

  private def saveSelected(): Unit =
    val names: Set[Band] =
      checkBoxes.iterator
        .filter(_.selected.value)
        .map(_.text.value: Band)
        .toSet

    availableBandsManager.save(names)

  // Now wire listeners (after checkBoxes is fully initialized)
  checkBoxes.foreach { cb =>
    cb.selected.onChange { (_, _, _) =>
      saveSelected()
    }
  }

  // Layout
  children = checkBoxes.map(_.asInstanceOf[Node])