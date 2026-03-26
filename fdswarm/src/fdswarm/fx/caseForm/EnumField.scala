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
import scalafx.collections.ObservableBuffer
import scalafx.scene.control.ComboBox
import scalafx.util.StringConverter

class EnumField extends FieldHandler:
  override def initialValue(value: Any): Unit =

    /** Handles Scala 3 enums via companion object reflection (e.g. ContestType$$anon$2). */
    /** Handle Scala 3 enums via companion reflection */
    val clazz = value.getClass
    val n = clazz.getName
    val dollar = n.indexOf('$')
    if dollar < 0 then
      throw new IllegalArgumentException(s"Not a Scala 3 enum case: $n")
    val enumTypeName = n.substring(0, dollar)
    val companionName = enumTypeName + "$"
    val companionClass = Class.forName(companionName)
    val module = companionClass.getField("MODULE$").get(null)
    val valuesMethod = companionClass.getMethod("values")
    val constants = valuesMethod.invoke(module).asInstanceOf[Array[AnyRef]]
    val options: Seq[AnyRef] = constants.toSeq
    val buffer: ObservableBuffer[AnyRef] = ObservableBuffer(options*)

    val initial = value.asInstanceOf[AnyRef]
    val combo = new ComboBox[AnyRef]() {
      items = buffer
      converter = new StringConverter[AnyRef] {
        override def toString(obj: AnyRef): String =
          if obj == null then
            ""
          else
            try
              val nameMethod = obj.getClass.getMethod("name")
              nameMethod.invoke(obj).asInstanceOf[String]
            catch
              case _: NoSuchMethodException => obj.toString

        override def fromString(string: String): AnyRef = null
      }
      this.value = initial
    }
    setControl(combo)

  override def getValue: Any = _control.get.asInstanceOf[ComboBox[AnyRef]].value.value
