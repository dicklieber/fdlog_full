package fdswarm.fx.utils.editor

import scalafx.beans.property.IntegerProperty
import scalafx.scene.Node
import scalafx.scene.control.{Spinner, TextFormatter}
import scalafx.util.StringConverter
import scalafx.Includes.*

class IntSpinner(
                  min: Int = 1,
                  max: Int = 100,
                  width: Double = 90.0,
                ) extends CustomFieldEditor:

  override def editor(fieldProperty: Any): Node =
    val intProp = fieldProperty.asInstanceOf[IntegerProperty]

    val spinner = new Spinner[Int](min, max, intProp.value, 1)
    spinner.editable = true
    spinner.prefWidth = width
    spinner.minWidth = width
    spinner.maxWidth = width

    val converter = new StringConverter[Integer]():
      override def toString(value: Integer): String =
        if value == null then "" else value.toString

      override def fromString(value: String): Integer =
        try
          if value == null || value.trim.isEmpty then
            Integer.valueOf(intProp.value)
          else
            Integer.valueOf(value.trim.toInt)
        catch
          case _: NumberFormatException =>
            Integer.valueOf(intProp.value)

    val filter = (change: TextFormatter.Change) =>
      val newText = change.getControlNewText
      if newText.matches("-?\\d*") then change else null

    spinner.getEditor.textFormatter =
      new TextFormatter[Integer](converter, Integer.valueOf(intProp.value), filter)

    spinner.value.onChange { (_, _, nv) =>
      intProp.value = nv
    }

    intProp.onChange { (_, _, nv) =>
      val newValue = nv.intValue
      if spinner.getValue != newValue then
        spinner.getValueFactory.setValue(newValue)
    }

    spinner