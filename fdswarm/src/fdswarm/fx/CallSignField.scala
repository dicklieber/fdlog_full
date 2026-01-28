
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

package fdswarm.fx

import _root_.scalafx.Includes.*
import _root_.scalafx.scene.control.TextField
import _root_.scalafx.scene.input.{KeyCode, KeyEvent}
import fdswarm.model.DupQsoDetector
import jakarta.inject.Inject

/**
 * Callsign entry field
 * sad or happy as validated while typing.
 *
 */
class CallSignField @Inject()( dupQsoDetector: DupQsoDetector) extends TextField with NextField {
  styleClass += "qsoCallSign"

/*  setFieldValidator(CallsignValidator)
    text.onChange { (_, _, nv) =>
      actionResult.clear()
      if (!validProperty.value) {
        if (text.value.isEmpty) {
  
        } else {
//          actionResult.potentiaDup(nv) //todo
        }
      }
    }*/ //todo: remove?

  onKeyPressed = { event =>
    val key: KeyCode = event.code
    if (key.isDigitKey && validProperty.value) {
      event.consume()
      val str: String = key.name
      onDoneFunction(str)
    }else{
      val text1 = event.text
//      actionResult.apply( dupQsoDetector(text.value))
    }
  }
  onKeyReleased = event => {
    val text1 = event.getText
//    event.toString
//    actionResult.apply( dupQsoDetector(text.value))
  }
//  onAction = event => {
//    actionResult.apply( dupQsoDetector(text.value))
//  }
}

