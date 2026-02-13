
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
    // The target regex is ^(?=.{3,12}$)[A-Z0-9]{1,3}[0-9][A-Z0-9]{1,4}(?:/[A-Z0-9]{1,4})?$
    // Partial regex must allow building up to this.
    val typingPattern = """^(?:[A-Z0-9]{0,3}(?:[0-9][A-Z0-9]{0,4}(?:/?[A-Z0-9]{0,4})?)?)?$"""
    if (newText.length <= 12 && newText.matches(typingPattern)) {
      change
    } else {
      null
    }
  })

  def isValid(str: String): Boolean =
    CallSignField.isValid(str)

object CallSignField:

  /**
   * regex that matches a valid callsign.
   *
   * Standard format: [A-Z0-9]{1,3}[0-9][A-Z0-9]{1,4}
   * Optional suffix: /[A-Z0-9]{1,4}
   * Total length restricted by lookahead: (?=.{3,12}$)
   */
  protected val regex: Regex = """^(?=.{3,12}$)[A-Z0-9]{1,3}[0-9][A-Z0-9]{1,4}(?:/[A-Z0-9]{1,4})?$""".r

  def isValid(str: String): Boolean =
    regex.findFirstIn(str).isDefined

