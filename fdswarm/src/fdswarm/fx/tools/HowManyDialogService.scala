package fdswarm.fx.tools

import fdswarm.store.BigQsosGenerator
import jakarta.inject.{Inject, Singleton}
import scalafx.Includes.*
import scalafx.beans.binding.Bindings
import scalafx.geometry.Insets
import scalafx.scene.control.*
import scalafx.scene.layout.GridPane
import scalafx.stage.Window

@Singleton
final class HowManyDialogService @Inject() (
                                             bigQsosGenerator: BigQsosGenerator
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

    val prefixField = new TextField {
      text = defaultPrefix
      promptText = "e.g. WA9"
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

      add(new Label("Callsign prefix:"), 0, 1)
      add(prefixField, 1, 1)
    }

    def validHowMany(s: String): Boolean =
      s.nonEmpty && s.forall(_.isDigit) && scala.util.Try(s.toInt).toOption.exists(_ > 0)

    def validPrefix(s: String): Boolean =
      s.nonEmpty && s.forall(_.isLetterOrDigit)

    val generateBtnNode = dialog.dialogPane().lookupButton(generateButtonType)

    val disableBinding = Bindings.createBooleanBinding(
      () =>
        !validHowMany(howManyField.text.value.trim) ||
          !validPrefix(prefixField.text.value.trim),
      howManyField.text,
      prefixField.text
    )
    generateBtnNode.disable <== disableBinding

    // IMPORTANT: return the pressed button so showAndWait has a non-null result
    dialog.resultConverter = (btn: ButtonType) => btn

    // Use delegate to avoid ScalaFX DConvert weirdness
    val opt = dialog.delegate.showAndWait() // java.util.Optional[ButtonType]
    if opt.isPresent && opt.get == generateButtonType then
      val howMany = howManyField.text.value.trim.toInt
      val prefix  = prefixField.text.value.trim.toUpperCase
      bigQsosGenerator.qsos(howMany, prefix)
  }
}