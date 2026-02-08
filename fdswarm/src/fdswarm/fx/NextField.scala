
/*
 * Copyright (C) 2021  Dick Lieber, WA9NNN
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

package fdswarm.fx

import _root_.scalafx.beans.binding.{Bindings, BooleanBinding}
import _root_.scalafx.beans.property.BooleanProperty
import _root_.scalafx.scene.control.TextInputControl
import com.typesafe.scalalogging.LazyLogging
import fdswarm.fx.InputHelper.forceCaps
import scalafx.scene.input.KeyCode
import scalafx.Includes.*
/**
 * Most of the common logic for any qso input field.
 */
trait NextField extends TextInputControl with WithDisposition with LazyLogging :
  forceCaps(this)
  styleClass += "qsoField"
  sad()

  def isValid(str: String): Boolean

  /**
   * Keys that will trigger transition to the next field if the current field is valid.
   */
  def isTransitionKey(key: KeyCode): Boolean =
    key.isDigitKey || key == KeyCode.Space || key == KeyCode.Enter || key == KeyCode.Tab

  var onDoneFunction: String => Unit =
    (_) => {}

  onKeyPressed = { event =>
    val key: KeyCode = event.code
    val isFieldValid = isValid(text.value)
    if (isFieldValid && isTransitionKey(key))
      event.consume()
      val str: String = NextField.toChar(key).toString
      onDoneFunction(str)
    else if (key == KeyCode.Enter || key == KeyCode.Tab)
      event.consume()
  }

  onKeyTyped = { event =>
    if (isValid(text.value))
      event.consume()
  }

  onKeyReleased = { event =>
    if (isValid(text.value))
      event.consume()
  }


//  def onDone(f: String => Unit): Unit = 
//    onDoneFunction = f

  val validProperty: BooleanProperty = new BooleanProperty()
  validProperty.value = false
  validProperty.onChange{(_,_,nv) =>
    disposition(nv)
  }

  def reset(): Unit = 
    text = ""

object NextField:
  def toChar(key: KeyCode): Char =
    key match
      case KeyCode.Space => ' '
      case KeyCode.Enter => '\n'
      case KeyCode.Tab   => '\t'
      case _ if key.isLetterKey => key.name.head
      case _ if key.isDigitKey  => key.name.last
      case _ => ' '

