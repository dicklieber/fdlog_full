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
import fdswarm.fx.tools.ZonedDateTimeEditor
import fdswarm.model.Callsign
import scalafx.Includes.*
import scalafx.collections.ObservableBuffer
import scalafx.geometry.Insets
import scalafx.scene.Node
import scalafx.scene.control.*
import scalafx.scene.layout.{GridPane, Pane, Region}
import scalafx.util.StringConverter

import java.time.{Instant, ZonedDateTime}
import scala.deriving.Mirror


/**
 *
 * @param initial  what we start with and will be editing.
 * @param controls mapping for field names to control. For special controls like contest class.
 *                 Overreds default type to control.
 * @param m
 * @tparam T
 */
class MyCaseForm[T <: Product](initial: T, controls: Seq[(String, FieldHandler)] = Nil)(using m: Mirror.ProductOf[T]):
  private val controlOverrideMap: Map[String, FieldHandler] = controls.toMap[String, FieldHandler]


  // fieldHandlers are in the same order as the fields in the case class.
  val fieldHandlers: IndexedSeq[FieldHandler] =
    for
      i <- 0 until initial.productArity
      fieldValue = initial.productElement(i)
      fieldName = initial.productElementName(i)
    yield
      controlOverrideMap.getOrElse(fieldName, handleDefault(fieldName, fieldValue)).setup(fieldName, fieldValue)

  def result: T =
    m.fromProduct(Tuple.fromArray(fieldHandlers.map(_.getValue).toArray))
  /*
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
    val fieldInfo = FieldInfo(name, control.asInstanceOf[scalafx.scene.control.Control])
    Field(name, fieldInfo.control, new Label(name) {
      minWidth = Region.USE_PREF_SIZE
    }, getter)
    */


  def pane(): Pane =
    val grid = new GridPane:
      hgap = 8
      vgap = 6
      padding = Insets(10)

    for
      (field, index) <- fieldHandlers.zipWithIndex
      control <- field.control()
    do
      grid.add(new Label(field.name) {
        minWidth = Region.USE_PREF_SIZE
      }, index, 0)
      grid.add(control, index, 1)

    grid
  
  /** For Scala 3 enums that are *not* Java enums at runtime (e.g. `enum HamBand(val ...)`). */
  private def scala3EnumItems(value: Any): Option[Seq[AnyRef]] =
    // Example runtime class name for a case: fdswarm.model.HamBand$B12m$
    val n = value.getClass.getName
    val dollar = n.indexOf('$')
    if dollar < 0 then None
    else
      val enumTypeName = n.substring(0, dollar) // -> fdswarm.model.HamBand
      val companionName = enumTypeName + "$" // -> fdswarm.model.HamBand$

      try
        val companionClass = Class.forName(companionName)
        val module = companionClass.getField("MODULE$").get(null)
        val valuesMethod = companionClass.getMethod("values")
        val values = valuesMethod.invoke(module).asInstanceOf[Array[?]]
        Some(values.iterator.map(_.asInstanceOf[AnyRef]).toSeq)
      catch
        case _: ReflectiveOperationException =>
          None

  private def handleDefault(fieldName: String, fieldValue: Any): FieldHandler = {
    val fieldHandler = fieldValue match {
      case _: String => new SimpleTextField()
      case _: Int => new SpinnerField()
      case _: ZonedDateTime => new ZonedDateTimeField()
      case _ => new SimpleTextField()
    }
    fieldHandler
  }
        
  /** For Java enums (and Scala 3 “simple” enums that compile to Java enums). */
  private def javaEnumItems(e: java.lang.Enum[?]): Seq[AnyRef] =
    val constants = e.getDeclaringClass.getEnumConstants
    constants.iterator.map(_.asInstanceOf[AnyRef]).toSeq

final case class Field[A](
                           name: String,
                           control: Node,
                           errorLabel: Label,
                           getValue: () => A
                         )

/**
 * A custom field e.g. contest class ComboBox.
 *
 */
trait FieldHandler {
  protected var _control: Option[Node] = None
  var name: String = ""
  /**
   * Handlers can provide a [[Node]] that is complex, not just a simple [[Control]].
   * None does not display the control
   *
   * @return
   */
  def control(): Option[Node] = _control

  def setControl(c: Node): Unit =
    _control = Option(c)

  /**
   * this must invoke [[setControl]]
   * @param value
   */
  def initialValue(value: Any): Unit
  def setup(name:String, value:Any): this.type =
    initialValue(value)
    this.name = name
    this
    
  def getValue: Any
}
