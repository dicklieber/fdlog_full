package fdswarm.fx.bands

import com.typesafe.scalalogging.LazyLogging
import fdswarm.model.BandMode.Band
import jakarta.inject.{Inject, Singleton}
import scalafx.geometry.Insets
import scalafx.scene.Node
import scalafx.scene.control.{CheckBox, TitledPane}
import scalafx.scene.layout.{Pane, VBox}

@Singleton
final class ModeCheckBoxPane @Inject()(
                                        availableModesManager: AvailableModesManager,
                                        modeCatalog: ModeCatalog
                                      ) extends LazyLogging:

  private val spacingPx = 6.0

  private val checkBoxes: Seq[CheckBox] =
    modeCatalog.modes.map { mode =>
      new CheckBox() {
        text = mode
        selected = availableModesManager.modes.contains(mode)
        selected.onChange { (a, b, c) =>
          logger.debug("Change: {} {} {}", a, b, c)
          saveSelected()
        }
      }
    }

  private def saveSelected(): Unit =
    val bands: Seq[Band] =
      checkBoxes.iterator
        .filter(_.selected.value)
        .map(_.text.value: Band)
        .toSeq

    availableModesManager.modes.setAll(bands*)

// Now wire listeners (after checkBoxes is fully initialized)
//  checkBoxes.foreach { cb =>
//    cb.selected.onChange { (_, _, _) =>
//      saveSelected()
//    }
//  }

// Layout

  val node: Node =
    new TitledPane() {
      text = "Modes"
      collapsible = false
      content = new VBox {
        spacing = 12.0
        children = checkBoxes
      }
    }