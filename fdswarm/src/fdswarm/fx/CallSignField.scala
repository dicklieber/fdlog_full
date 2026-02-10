
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
import _root_.scalafx.scene.control.{TextField, TextFormatter}
import _root_.scalafx.scene.input.{KeyCode, KeyEvent}
import fdswarm.fx.CallSignField.regex
import fdswarm.fx.bandmodes.{BandModeStore, SelectedBandModeStore}
import fdswarm.store.QsoStore
import jakarta.inject.Inject

import scala.util.matching.Regex

/**
 * Callsign entry field
 * sad or happy as validated while typing.
 *
 */
class CallSignField @Inject()(qsoStore: QsoStore, selectedBsndModeStore: SelectedBandModeStore) extends TextField with NextField:
  styleClass += "qsoCallSign"

  text.onChange((_, _, newText) => {
    if (isValid(newText)) {
      if (!styleClass.contains("callSignOk")) {
        styleClass += "callSignOk"
      }
    } else {
      styleClass -= "callSignOk"
    }
  })

  textFormatter = new TextFormatter[String]((change: TextFormatter.Change) => {
    if (change.isContentChange) {
      change.setText(change.getText.toUpperCase)
    }
    val newText = change.controlNewText
    // Match partial strings during typing.
    // The target regex is ^[A-Z0-9]{1,3}[0-9][A-Z]{1,3}$
    // Partial regex:
    // 1-3 A-Z0-9
    // followed by a digit
    // followed by 1-3 A-Z
    val typingPattern = "^([A-Z0-9]{1,3}[0-9]?[A-Z]{0,3})?$"
    if (newText.matches(typingPattern)) {
      change
    } else {
      null
    }
  })

  def isValid(str: String): Boolean =
    CallSignField.isValid(str)

object CallSignField:

  protected val regex: Regex = """^[A-Z0-9]{1,3}[0-9][A-Z]{1,3}$""".r

  def isValid(str: String): Boolean =
    regex.findFirstIn(str).isDefined

