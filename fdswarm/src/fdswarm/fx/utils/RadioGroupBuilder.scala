package fdswarm.fx.utils

import scala.reflect.{ClassTag, Enum as ScalaEnum}
import scalafx.Includes.*
import scalafx.beans.property.ObjectProperty
import scalafx.scene.control.{RadioButton, ToggleGroup}
import scalafx.scene.layout.HBox

final case class RadioGroup[E](
                                box: HBox,
                                selected: ObjectProperty[E]
                              )

object RadioGroupBuilder:

  def forValues[E <: AnyRef](
                              values: Seq[E],
                              initial: E,
                              label: E => String = (e: E) => e.toString,
                              spacingValue: Double = 10.0
                            ): RadioGroup[E] =

    val selectedProperty = ObjectProperty[E](initial)
    val tg = new ToggleGroup()

    val buttons: Seq[RadioButton] =
      values.map { value =>
        new RadioButton {
          text = label(value)
          userData = value
          toggleGroup = tg
          selected = value == initial
        }
      }

    tg.selectedToggle.onChange { (_, _, newToggle) =>
      if newToggle != null then
        selectedProperty.value =
          newToggle.userData.asInstanceOf[E]
    }

    val box = new HBox:
      spacing = spacingValue

    buttons.foreach(button => box.children.add(button.delegate))

    RadioGroup(box, selectedProperty)

  def forScalaEnum[E <: ScalaEnum & AnyRef](
                                             initial: E,
                                             label: E => String = (e: E) => e.toString,
                                             spacingValue: Double = 10.0
                                           )(using ct: ClassTag[E]): RadioGroup[E] =
    val enumClass = ct.runtimeClass
    val companionField = enumClass.getDeclaredField("MODULE$")
    companionField.setAccessible(true)
    val companion = companionField.get(null)
    val valuesMethod = companion.getClass.getMethod("enumValues")
    val rawValues = valuesMethod.invoke(companion)
    val values = rawValues.asInstanceOf[Array[E]].toSeq

    forValues(
      values = values,
      initial = initial,
      label = label,
      spacingValue = spacingValue
    )