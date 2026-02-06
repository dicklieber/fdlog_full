
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
import fdswarm.fx.CallSignField.regex
import fdswarm.model.DupQsoDetector
import jakarta.inject.Inject

import scala.util.matching.Regex

/**
 * Callsign entry field
 * sad or happy as validated while typing.
 *
 */
class CallSignField @Inject()(dupQsoDetector: DupQsoDetector) extends TextField with NextField :
  styleClass += "qsoCallSign"

  //  def onNextField(ch:Char => Unit):Unit=
  //    onKeyReleased = event => ch(event.getText.head)


  def isValid(str: String): Boolean = CallSignField.isValid(str)

object CallSignField:
  def isValid(str: String): Boolean = regex.findFirstIn(str).isDefined
  protected val regex: Regex = """^(?=.{3,12}$)[A-Z0-9]{1,3}[0-9][A-Z0-9]{1,4}(?:/(?:P|M|MM|AM|QRP|[A-Z0-9]{1,4}))?$""".r

