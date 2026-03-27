package fdswarm.fx.utils.editor

import scalafx.beans.property.{IntegerProperty, Property}
import scalafx.scene.Node
import scalafx.scene.control.Spinner

class IntSpinner(
                  min: Int = 1,
                  max: Int = 100,
                ) extends CustomFieldEditor:

  override def editor(fieldProperty: Property[?, ?]): Node =
    fieldProperty match
      case intProp: IntegerProperty =>
        val spinner = new Spinner[Int](min, max, intProp.value, 1)

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