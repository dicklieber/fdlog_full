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

package fdswarm.fx.caseForm

import fdswarm.fx.InputHelper.*
import fdswarm.model.Callsign
import javafx.event.{ActionEvent as JfxActionEvent, EventHandler as JfxEventHandler}
import scalafx.Includes.*
import scalafx.collections.ObservableBuffer
import scalafx.geometry.Insets
import scalafx.scene.Node
import scalafx.scene.control.*
import scalafx.scene.layout.{GridPane, HBox, Pane, Region}
import scalafx.util.StringConverter

import java.time.{Instant, LocalTime, ZoneId, ZonedDateTime}
import fdswarm.fx.tools.ZonedDateTimeEditor
import scala.deriving.Mirror

/**
 * A "field value" that knows:
 *  - the current selected value
 *  - how to build a Spinner
 *
 * This is meant to be embedded in your case class.
 */
final case class SpinnerField(
    value: Int,
    min: Int,
    max: Int
):
  def spinner(): Spinner[Int] =
    new Spinner[Int](min, max, value) {
      editable = true
      editor.value.textFormatter = new TextFormatter[String]((change: TextFormatter.Change) => {
        if (change.isContentChange) {
          val newText = change.controlNewText
          if (newText.matches("-?\\d*")) {
            change
          } else {
            null
          }
        } else {
          change
        }
      })
    }

class MyCaseForm[T <: Product](initial: T)(using m: Mirror.ProductOf[T]):

  /** For Scala 3 enums that are *not* Java enums at runtime (e.g. `enum HamBand(val ...)`). */
  private def scala3EnumItems(value: Any): Option[Seq[AnyRef]] =
    // Example runtime class name for a case: fdswarm.model.HamBand$B12m$
    val n = value.getClass.getName
    val dollar = n.indexOf('$')
    if dollar < 0 then None
    else
      val enumTypeName  = n.substring(0, dollar) // -> fdswarm.model.HamBand
      val companionName = enumTypeName + "$"     // -> fdswarm.model.HamBand$

      try
        val companionClass = Class.forName(companionName)
        val module         = companionClass.getField("MODULE$").get(null)
        val valuesMethod   = companionClass.getMethod("values")
        val values         = valuesMethod.invoke(module).asInstanceOf[Array[?]]
        Some(values.iterator.map(_.asInstanceOf[AnyRef]).toSeq)
      catch
        case _: ReflectiveOperationException =>
          None

  /** For Java enums (and Scala 3 “simple” enums that compile to Java enums). */
  private def javaEnumItems(e: java.lang.Enum[?]): Seq[AnyRef] =
    val constants = e.getDeclaringClass.getEnumConstants
    constants.iterator.map(_.asInstanceOf[AnyRef]).toSeq

  val fields: IndexedSeq[Field[Any]] =
    for
      i <- 0 until initial.productArity
      fieldValue = initial.productElement(i)
      if !fieldValue.isInstanceOf[Instant] && !fieldValue.isInstanceOf[fdswarm.ContestDates] // keep Instants and ContestDates as-is
    yield
      val (control, getter): (Node, () => Any) =
        fieldValue match
          case zdt: ZonedDateTime =>
            val name = initial.productElementName(i)
            val editor = new ZonedDateTimeEditor(zdt, name)
            (editor, () => editor.value)

          // ---- Provided-choice support (e.g. HamBand from AvailableBandsStore) ----
          case cf: ChoiceField[?] =>
            val cf0   = cf.asInstanceOf[ChoiceField[AnyRef]]
            val combo = cf0.comboBox().asInstanceOf[ComboBox[AnyRef]]
            (combo, () => cf0.withValue(combo.value.value))

          case sf: SpinnerField =>
            val spinner = sf.spinner()
            (spinner, () => sf.copy(value = spinner.value.value))

          case _ =>
            scala3EnumItems(fieldValue) match
              case Some(items) =>
                val combo = new ComboBox[AnyRef](ObservableBuffer.from(items)) {
                  converter = new StringConverter[AnyRef] {
                    override def toString(obj: AnyRef): String =
                      if (obj == null) ""
                      else
                        try {
                          val nameMethod = obj.getClass.getMethod("name")
                          nameMethod.invoke(obj).asInstanceOf[String]
                        } catch {
                          case _: NoSuchMethodException => obj.toString
                        }
                    override def fromString(string: String): AnyRef = null // read-only ComboBox typically
                  }
                }
                combo.value.value = fieldValue.asInstanceOf[AnyRef]
                (combo, () => combo.value.value)

              case None =>
                fieldValue match
                  case cs: Callsign =>
                    val tf = new TextField:
                      text = cs.value
                      promptText = "Enter callsign"
                    forceCaps(tf)
                    (tf, () => Callsign(tf.text.value.trim))

                  case e: java.lang.Enum[_] =>
                    val items = javaEnumItems(e)
                    val combo = new ComboBox[AnyRef](ObservableBuffer.from(items)) {
                      converter = new StringConverter[AnyRef] {
                        override def toString(obj: AnyRef): String =
                          if (obj == null) ""
                          else
                            try {
                              val nameMethod = obj.getClass.getMethod("name")
                              nameMethod.invoke(obj).asInstanceOf[String]
                            } catch {
                              case _: NoSuchMethodException => obj.toString
                            }
                        override def fromString(string: String): AnyRef = null
                      }
                    }
                    combo.value.value = e.asInstanceOf[AnyRef]
                    (combo, () => combo.value.value)

                  case cf: ChoiceField[?] =>
                    val cf0   = cf.asInstanceOf[ChoiceField[AnyRef]]
                    val combo = cf0.comboBox().asInstanceOf[ComboBox[AnyRef]]
                    (combo, () => cf0.withValue(combo.value.value))
                  case b: Boolean =>
                    val cb = new CheckBox:
                      selected = b
                    (cb, () => cb.selected.value)

                  case s: String =>
                    val tf = new TextField:
                      text = s
                    (tf, () => tf.text.value)

                  case i0: Int =>
                    val tf = new TextField:
                      text = i0.toString
                    (tf, () => tf.text.value.trim.toInt)

                  case l0: Long =>
                    val tf = new TextField:
                      text = l0.toString
                    (tf, () => tf.text.value.trim.toLong)

                  case d0: Double =>
                    val tf = new TextField:
                      text = d0.toString
                    (tf, () => tf.text.value.trim.toDouble)

                  case other =>
                    throw new IllegalArgumentException(
                      s"Unsupported field type: $other (${other.getClass.getName})"
                    )

      val name = initial.productElementName(i)
      Field(name, control, new Label(name) { minWidth = Region.USE_PREF_SIZE }, getter)

  def pane(): Pane =
    val grid = new GridPane:
      hgap = 8
      vgap = 6
      padding = Insets(10)

    fields.zipWithIndex.foreach { case (field, index) =>
      grid.add(new Label(field.name) { minWidth = Region.USE_PREF_SIZE }, index, 0)
      grid.add(field.control, index, 1)
    }

    grid
  
  def control[T <: Control](name: String): T =
    fields.find(_.name == name).get.control.asInstanceOf[T]

  def result: T =
    val values: Array[Any] = new Array[Any](initial.productArity)

    for i <- 0 until initial.productArity do
      initial.productElement(i) match
        case inst: Instant =>
          values(i) = inst // keep original Instants
        case cd: fdswarm.ContestDates =>
          values(i) = cd
        case _ =>
          val name  = initial.productElementName(i)
          val field = fields.find(_.name == name).get
          values(i) = field.getValue()

    m.fromProduct(Tuple.fromArray(values))

final case class Field[A](
                           name: String,
                           control: Node,
                           errorLabel: Label,
                           getValue: () => A
                         )