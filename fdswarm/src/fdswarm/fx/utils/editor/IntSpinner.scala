package fdswarm.fx.utils.editor

import scalafx.beans.property.{IntegerProperty, Property}
import scalafx.scene.Node
import scalafx.scene.control.{Spinner, TextFormatter}
import scalafx.util.StringConverter
import scalafx.Includes.jfxTextField2sfx

class IntSpinner(
                  min: Int = 1,
                  max: Int = 100,
                  width: Double = 90.0,
                ) extends CustomFieldEditor:

  override def editor(fieldProperty: Property[?, ?]): Node =
    fieldProperty match
      case intProp: IntegerProperty =>
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
              if value == null || value.trim.isEmpty then null.asInstanceOf[Integer]
              else java.lang.Integer.valueOf(java.lang.Integer.parseInt(value.trim))
            catch
              case _: NumberFormatException => null.asInstanceOf[Integer]

        val filter = (change: TextFormatter.Change) =>
          val newText = change.controlNewText
          if newText.matches("-?\\d*") then change else null.asInstanceOf[TextFormatter.Change]

        spinner.editor().textFormatter = TextFormatter(converter, intProp.value, filter)

        spinner.value.onChange { (_, _, nv) =>
          intProp.value = nv
        }

        intProp.onChange { (_, _, nv) =>
          val newValue = nv.intValue
          if spinner.getValue != newValue then
            spinner.getValueFactory.setValue(newValue)
        }

        spinner

      case other =>
        throw new IllegalArgumentException(
          s"IntSpinner requires IntegerProperty, got: ${other.getClass.getName}"
        )
