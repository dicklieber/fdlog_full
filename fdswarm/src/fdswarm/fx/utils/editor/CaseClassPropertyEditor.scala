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

package fdswarm.fx.utils.editor

import scalafx.beans.property.*
import scalafx.scene.Node
import scalafx.scene.control.*
import scalafx.scene.layout.*

import java.lang.reflect.Constructor
import scala.collection.mutable
import fdswarm.util.camelToWords


class CaseClassPropertyEditor[T <: Product](
                                              val target: ObjectProperty[T]
                                            ):

  require(target != null, "target must not be null")
  require(target.value != null, "target.value must not be null")

  private val initialValue: T =
    target.value

  private val runtimeClass: Class[?] =
    initialValue.getClass

  private val fieldNames: Vector[String] =
    initialValue.productElementNames.toVector

  private val propertiesInOrder: Vector[(String, Property[?, ?])] =
    buildProperties(initialValue)

  private val propertiesByName: Map[String, Property[?, ?]] =
    propertiesInOrder.toMap

  private val customEditors =
    mutable.LinkedHashMap.empty[String, CustomFieldEditor]

  def getProperty[A](propertyName: String): A =
    propertiesByName.getOrElse(
      propertyName,
      throw new NoSuchElementException(s"No field named '$propertyName'")
    ).asInstanceOf[A]

  def setCustomEditor(fieldName: String, editor: CustomFieldEditor): Unit =
    require(fieldNames.contains(fieldName), s"No field named '$fieldName'")
    customEditors(fieldName) = editor

  def horizontal: Pane =
    new GridPane:
      hgap = 12
      vgap = 8

      for ((fieldName, property), col) <- propertiesInOrder.zipWithIndex do
        add(new Label(camelToWords(fieldName)), col, 0)
        add(nodeFor(fieldName, property), col, 1)

  def vertical: Pane =
    new GridPane:
      hgap = 8
      vgap = 8

      for ((fieldName, property), row) <- propertiesInOrder.zipWithIndex do
        add(new Label(camelToWords(fieldName)), 0, row)
        add(nodeFor(fieldName, property), 1, row)

  def finish(): Unit =
    val rebuilt = rebuildTarget().asInstanceOf[T]
    target.value = rebuilt

  private def nodeFor(fieldName: String, property: Property[?, ?]): Node =
    customEditors.get(fieldName) match
      case Some(custom) => custom.editor(property)
      case None         => defaultEditor(property)

  private def buildProperties(value: T): Vector[(String, Property[?, ?])] =
    val values = value.productIterator.toVector
    fieldNames.zip(values).map { (name, fieldValue) =>
      name -> newProperty(fieldValue)
    }

  private def newProperty(value: Any): Property[?, ?] =
    value match
      case v: String  => StringProperty(v)
      case v: Int     => IntegerProperty(v)
      case v: Long    => LongProperty(v)
      case v: Double  => DoubleProperty(v)
      case v: Boolean => BooleanProperty(v)
      case v          => ObjectProperty[Any](v)

  private def defaultEditor(property: Property[?, ?]): Node =
    property match
      case p: StringProperty =>
        new TextField:
          text <==> p

      case p: IntegerProperty =>
        new TextField:
          text = p.value.toString
          text.onChange { (_, _, nv) =>
            parseIntValue(nv).foreach(v => p.value = v)
          }

      case p: LongProperty =>
        new TextField:
          text = p.value.toString
          text.onChange { (_, _, nv) =>
            parseLongValue(nv).foreach(v => p.value = v)
          }

      case p: DoubleProperty =>
        new TextField:
          text = p.value.toString
          text.onChange { (_, _, nv) =>
            parseDoubleValue(nv).foreach(v => p.value = v)
          }

      case p: BooleanProperty =>
        new CheckBox:
          selected <==> p

      case p: ObjectProperty[?] =>
        new TextField:
          text = Option(p.value).map(_.toString).getOrElse("")
          disable = true
          promptText = "Unsupported type for direct editing"

  private def rebuildTarget(): Any =
    val ctor = primaryConstructor(runtimeClass)
    val args = propertiesInOrder.map { (_, property) =>
      propertyValue(property)
    }
    ctor.newInstance(args*)

  private def propertyValue(property: Property[?, ?]): Object =
    property match
      case p: StringProperty    => p.value
      case p: IntegerProperty   => Int.box(p.value)
      case p: LongProperty      => Long.box(p.value)
      case p: DoubleProperty    => Double.box(p.value)
      case p: BooleanProperty   => Boolean.box(p.value)
      case p: ObjectProperty[?] => p.value.asInstanceOf[Object]

  private def primaryConstructor(clazz: Class[?]): Constructor[?] =
    val ctor = clazz.getDeclaredConstructors.maxBy(_.getParameterCount)
    ctor.setAccessible(true)
    ctor

  private def parseIntValue(s: String): Option[Int] =
    try Some(s.trim.toInt)
    catch
      case _: NumberFormatException => None

  private def parseLongValue(s: String): Option[Long] =
    try Some(s.trim.toLong)
    catch
      case _: NumberFormatException => None

  private def parseDoubleValue(s: String): Option[Double] =
    try Some(s.trim.toDouble)
    catch
      case _: NumberFormatException => None

trait CustomFieldEditor:
  def editor(fieldProperty: Property[?, ?]): Node
