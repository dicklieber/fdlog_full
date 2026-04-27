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

package fdswarm.fx.components

import javafx.scene.control.TextFormatter.Change
import scalafx.beans.property.ObjectProperty
import scalafx.scene.control.{TextField, TextFormatter}

import java.util.function.UnaryOperator

class OptionTextField(initial: Option[String] = None) extends TextField:

  text = initial.map(_.toUpperCase).getOrElse("")

  textFormatter = new TextFormatter[String](
    new UnaryOperator[Change]:
      override def apply(change: Change): Change =
        if change.getText != null then
          change.setText(change.getText.toUpperCase)
        change
  )

  def value: Option[String] =
    Option(text.value).map(_.trim).filter(_.nonEmpty)

  def value_=(v: Option[String]): Unit =
    text = v.map(_.toUpperCase).getOrElse("")

  val optionValueProperty: ObjectProperty[Option[String]] =
    ObjectProperty[Option[String]](this, "optionValue", value)

  text.onChange { (_, _, _) =>
    optionValueProperty.value = value
  }

  optionValueProperty.onChange { (_, _, newValue) =>
    val newText = newValue.map(_.toUpperCase).getOrElse("")
    if text.value != newText then
      text = newText
  }