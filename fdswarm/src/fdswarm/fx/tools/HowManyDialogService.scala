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
import fdswarm.fx.utils.IconButton
import fdswarm.store.BigQsosGenerator
import jakarta.inject.{Inject, Singleton}
import scalafx.Includes.*
import scalafx.beans.binding.Bindings
import scalafx.geometry.Insets
import scalafx.scene.control.*
import scalafx.scene.layout.{GridPane, HBox, Priority}
import scalafx.scene.paint.Color
import scalafx.stage.Window

@Singleton
final class HowManyDialogService @Inject() (
                                             bigQsosGenerator: BigQsosGenerator,
                                             qsoStore: fdswarm.store.QsoStore
                                           ) {

  def showAndGenerate(
                       ownerWindow: Window,
                       defaultHowMany: Int = 100,
                       defaultPrefix: String = "WA9"
                     ): Unit = {

    val howManyField = new TextField {
      text = defaultHowMany.toString
      promptText = "e.g. 100"
    }

    val howManyPerHourField = new TextField {
      text = "60"
      promptText = "e.g. 60"
    }

    val prefixField = new TextField {
      text = defaultPrefix
      promptText = "e.g. WA9"
    }
    forceCaps(prefixField)

    val removeAllButton = IconButton("trash3-fill", 24, "Remove all Qsos loggers", Color.Red)
    removeAllButton.onAction = _ => {
      val confirm = new Alert(Alert.AlertType.Confirmation) {
        initOwner(ownerWindow)
        title = "Confirm Deletion"
        headerText = "Delete all QSOs?"
        contentText = "This will permanently delete all QSOs and the journal file."
      }
      val result = confirm.showAndWait()
      if (result.contains(ButtonType.OK)) {
        qsoStore.removeAll()
      }
    }

    // Return ButtonType, not a value (no null-result headaches)
    val dialog = new Dialog[ButtonType] {
      title = "Generate QSOs"
      headerText = "Generate synthetic QSOs"
      initOwner(ownerWindow)
    }

    val generateButtonType = new ButtonType("Generate", ButtonBar.ButtonData.OKDone)
    dialog.dialogPane().buttonTypes = Seq(ButtonType.Cancel, generateButtonType)

    dialog.dialogPane().content = new GridPane {
      hgap = 10
      vgap = 10
      padding = Insets(10)

      add(new Label("How many:"), 0, 0)
      add(howManyField, 1, 0)

      add(new Label("How many per hour:"), 0, 1)
      add(howManyPerHourField, 1, 1)

      add(new Label("Callsign prefix:"), 0, 2)
      add(prefixField, 1, 2)

      add(removeAllButton, 0, 3)
    }

    def validHowMany(s: String): Boolean =
      s.nonEmpty && s.forall(_.isDigit) && scala.util.Try(s.toInt).toOption.exists(_ > 0)

    def validPrefix(s: String): Boolean =
      s.nonEmpty && s.forall(_.isLetterOrDigit)

    val generateBtnNode = dialog.dialogPane().lookupButton(generateButtonType)

    val disableBinding = Bindings.createBooleanBinding(
      () =>
        !validHowMany(howManyField.text.value.trim) ||
          !validHowMany(howManyPerHourField.text.value.trim) ||
          !validPrefix(prefixField.text.value.trim),
      howManyField.text,
      howManyPerHourField.text,
      prefixField.text
    )
    generateBtnNode.disable <== disableBinding

    // IMPORTANT: return the pressed button so showAndWait has a non-null result
    dialog.resultConverter = (btn: ButtonType) => btn

    // Use delegate to avoid ScalaFX DConvert weirdness
    val opt = dialog.delegate.showAndWait() // java.util.Optional[ButtonType]
    if opt.isPresent && opt.get == generateButtonType then
      val howMany = howManyField.text.value.trim.toInt
      val howManyPerHour = howManyPerHourField.text.value.trim.toInt
      val prefix  = prefixField.text.value.trim.toUpperCase
      bigQsosGenerator.qsos(howMany, howManyPerHour, prefix)
  }
}