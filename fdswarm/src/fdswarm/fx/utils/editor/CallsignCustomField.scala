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

package fdswarm.fx.utils.editor

import scalafx.beans.property.{ObjectProperty, Property}
import scalafx.scene.Node
import scalafx.scene.control.TextField
import fdswarm.model.Callsign

class CallsignCustomField extends CustomFieldEditor:
  override def editor(fieldProperty: Property[?, ?]): Node = fieldProperty match
    case prop: ObjectProperty[?] =>
      val callsignProp = prop.asInstanceOf[ObjectProperty[Callsign]]
      val tf = new TextField {
        text.onChange { (_, _, newValue) =>
          val filtered = newValue.filter(c => c.isLetterOrDigit || c == '/').toUpperCase
          if filtered != newValue then
            text.value = filtered
        }
      }
      // from prop to text
      callsignProp.onChange { (_, _, newVal) =>
        val textVal = if newVal == null then "" else newVal.toString
        if tf.text.value != textVal then
          tf.text.value = textVal
      }
      // initial
      tf.text.value = if callsignProp.value == null then "" else callsignProp.value.toString
      // from text to prop
      tf.text.onChange { (_, _, _) =>
        val textVal = tf.text.value.trim
        val cs = if textVal.isEmpty then null.asInstanceOf[Callsign] else Callsign(textVal)
        callsignProp.value = cs
      }
      tf
    case other =>
      throw new IllegalArgumentException(s"CallsignCustomField requires ObjectProperty[Callsign], got ${other.getClass.getName}")