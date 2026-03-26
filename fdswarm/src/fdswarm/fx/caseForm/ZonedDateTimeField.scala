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

import fdswarm.fx.tools.ZonedDateTimeEditor
import scalafx.scene.Node
import scalafx.scene.control.Control
import scalafx.scene.layout.HBox

import java.time.ZonedDateTime

/**
 * A field that edits a ZonedDateTime.
 * displays the ZonedDateTime with button to popup an dialig.
 */
class ZonedDateTimeField extends FieldHandler:
  /**
   * The control that is used to edit the field.
   * None does not display the control
   *
   * @return
   */
  private var maybeZdt: Option[ZonedDateTimeEditor] = None
  
  override def initialValue(value: Any): Unit =
    val zdt = value.asInstanceOf[ZonedDateTime]
    val editor = new ZonedDateTimeEditor(zdt, "Date/Time")
    maybeZdt = Option(editor)
    setControl(editor)

  override def getValue: Any =
    maybeZdt.get.value

