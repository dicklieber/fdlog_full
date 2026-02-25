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

package fdswarm.fx.qso

import fdswarm.fx.{NextField, UserConfig}
import fdswarm.fx.contest.{ContestCatalog, ContestManager}
import jakarta.inject.*
import scalafx.Includes.*
import scalafx.scene.control.{TextField, TextFormatter}

class ContestClassField @Inject() (
    contestManager: ContestManager,
    contestCatalog: ContestCatalog,
    dupPanel: DupPanel,
    override val userConfig: UserConfig
) extends TextField
    with NextField:
  logger.trace("ctor")

  private def showHelp(): Unit =
    val currentContest = contestManager.config.contest
    val contest = contestCatalog.contests.find(_.name == currentContest)
    contest.foreach { c =>
      val items = c.classChars.map(cc => (cc.ch, cc.description))
      dupPanel.show(s"$currentContest Classes", items)
    }

  focused.onChange { (_, _, nv) =>
    val currentText = text.value
    val classChars = contestManager.currentDetailProperty.value.classChars
    val typingPattern = "^([0-9]{1,2}[" + classChars.toUpperCase + "]|[0-9]{0,2})$"
    if nv && !currentText.matches(typingPattern) then showHelp()
  }

  textFormatter = new TextFormatter[String]((change: TextFormatter.Change) => {
    if (change.isContentChange) {
      change.setText(change.getText.toUpperCase)
    }
    val newText = change.controlNewText
    val classChars = contestManager.currentDetailProperty.value.classChars
    // Match partial strings during typing: empty, 1-2 digits, or 1-2 digits + 1 classChar
    val typingPattern = "^([0-9]{1,2}[" + classChars.toUpperCase + "]|[0-9]{0,2})$"
    if (newText.matches(typingPattern)) {
      change
    } else {
      // Only show help if the rejected character makes it clearly invalid,
      // and not just a prefix that could be valid.
      // For ContestClassField, typingPattern already allows 0-2 digits.
      // If it doesn't match, it means it's definitely wrong for the pattern.
      showHelp()
      null
    }
  })

  text.onChange { (_, _, nv) =>
    validProperty.value = isValid(nv)
    val classChars = contestManager.currentDetailProperty.value.classChars
    val typingPattern = "^([0-9]{1,2}[" + classChars.toUpperCase + "]|[0-9]{0,2})$"
    if nv.matches(typingPattern) then dupPanel.clear
  }

  override def isTransitionKey(key: scalafx.scene.input.KeyCode): Boolean =
    super.isTransitionKey(key) || key.isLetterKey

  override def isValid(str: String): Boolean =
    val classChars = contestManager.currentDetailProperty.value.classChars
    val pattern = "^[0-9]{1,2}[" + classChars.toUpperCase + "]$"
    str.matches(pattern)
