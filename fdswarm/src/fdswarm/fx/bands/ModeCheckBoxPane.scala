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

package fdswarm.fx.bands

import fdswarm.bands.{AvailableModesManager, ModeCatalog}
import fdswarm.logging.LazyStructuredLogging
import fdswarm.fx.GridColumns
import fdswarm.fx.bands.*
import fdswarm.model.BandMode.Mode
import jakarta.inject.{Inject, Singleton}
import scalafx.geometry.Insets
import scalafx.scene.Node
import scalafx.scene.control.{CheckBox, Label, Tooltip}
import scalafx.scene.layout.{Pane, VBox}

@Singleton
final class ModeCheckBoxPane @Inject()(
                                        availableModesManager: AvailableModesManager,
                                        modeCatalog: ModeCatalog
                                      ) extends LazyStructuredLogging:

  private val spacingPx = 6.0

  private val checkBoxes: Seq[CheckBox] =
    modeCatalog.modes.map { mode =>
      new CheckBox() {
        text = mode
        selected = availableModesManager.modes.contains(mode)
        selected.onChange { (a, b, c) =>
          logger.debug(s"Change: a:$a b:$b c:$c")
          saveSelected()
        }
      }
    }

  private def saveSelected(): Unit =
    val modes: Seq[Mode] =
      checkBoxes.iterator
        .filter(_.selected.value)
        .map(_.text.value: Mode)
        .toSeq

    availableModesManager.modes.setAll(modes*)

// Now wire listeners (after checkBoxes is fully initialized)
//  checkBoxes.foreach { cb =>
//    cb.selected.onChange { (_, _, _) =>
//      saveSelected()
//    }
//  }

// Layout
  private val headerLabel = new Label("Modes") {
    style = "-fx-cursor: hand; -fx-text-fill: derive(-fx-accent, -20%); -fx-underline: true;"
    tooltip = Tooltip("Click to toggle all modes")
    onMouseClicked = _ => {
      val allChecked = checkBoxes.forall(_.selected.value)
      val newState = !allChecked
      checkBoxes.foreach(_.selected.value = newState)
      saveSelected()
    }
  }

  val node: Node =
    GridColumns.fieldSet(
      headerLabel,
      new VBox {
        spacing = 12.0
        children = checkBoxes
      }
    )
