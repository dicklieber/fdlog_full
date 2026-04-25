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

package fdswarm.fx.sections

import fdswarm.fx.{NextField, UserConfig}
import jakarta.inject.{Inject, Singleton}
import scalafx.Includes.*
import scalafx.scene.control.{TextField, TextFormatter}

class SectionField @Inject()(sectionsProvider: SectionsProvider, override val userConfig: UserConfig) extends TextField with NextField:

  text.onChange { (_, _, nv) =>
    validProperty.value = isValid(nv)
  }

  textFormatter = new TextFormatter[String]((change: TextFormatter.Change) => {
    if (change.isContentChange) {
      change.setText(change.getText.toUpperCase)
    }
    val newText = change.controlNewText
    if (newText.isEmpty) change
    else {
      val upper = newText.trim.toUpperCase
      val isValidPartial = sectionsProvider.allSections.exists(_.code.toUpperCase.startsWith(upper))
      if (isValidPartial) change
      else null
    }
  })

  override def isValid(str: String): Boolean =
    if str == null then false
    else
      val upper = str.trim.toUpperCase
      sectionsProvider.allSections.exists(_.code.toUpperCase == upper)

  def uniqueMatchingCode(str: String): Option[String] =
    val upper = Option(str).getOrElse("").trim.toUpperCase
    if upper.isEmpty then None
    else
      val matches = sectionsProvider.allSections
        .map(_.code.toUpperCase)
        .distinct
        .filter(_.startsWith(upper))
      if matches.size == 1 then Some(matches.head) else None

  def applyUniqueMatchForCurrentInput(): Boolean =
    uniqueMatchingCode(text.value) match
      case Some(code) =>
        text = code
        true
      case None => false
