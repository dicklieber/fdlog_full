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

import fdswarm.model.Selectable
import scalafx.Includes.*
import scalafx.collections.ObservableBuffer
import scalafx.scene.control.{ComboBox, ListCell, ListView}
import scalafx.util.StringConverter

/**
 * A ComboBox that includes an "-any-" option as None, and formats other options as "value - label".
 * It is sized based on the rendered value (T or "-any-"), while the dropdown can be wider.
 *
 * @param choices A vararg of Selectable[T] instances.
 * @tparam T The type of the value.
 */
class AnyComboBox[T](choices: Selectable[T]*) extends ComboBox[Option[T]] {

  private val anyText = "-any-"
  private val data = (None -> anyText) +: choices.map { s => Some(s.value) -> s"${s.value} - ${s.label}" }
  private val map = data.toMap

  items = ObservableBuffer.from(data.map(_._1))
  value = None

  // The requirement says: "The combox as rendered should be sized based on the T or -any-
  // not the width of the choices. Choices should be as wide as needed, when comboBox is open"

  // cellFactory is used for rendering the items in the dropdown.
  cellFactory = (lv: ListView[Option[T]]) =>
    new ListCell[Option[T]] {
      item.onChange { (_, _, it) =>
        text = map.getOrElse(it, "")
      }
    }

  converter = new StringConverter[Option[T]] {
    override def toString(opt: Option[T]): String = opt match {
      case None => anyText
      case Some(v) => v.toString
    }
    override def fromString(s: String): Option[T] =
      if (s == anyText) None
      else choices.find(_.value.toString == s).map(c => Some(c.value)).getOrElse(None)
  }

  // buttonCell is used for rendering the ComboBox itself (the button part).
  // JavaFX ComboBox uses the items + buttonCell/converter to compute its preferred width.
  // By using a buttonCell that only displays the short text, and having a converter
  // that also returns the short text, the ComboBox will size itself to the widest
  // of these short strings, satisfying the requirement.
  buttonCell = new ListCell[Option[T]] {
    item.onChange { (_, _, it) =>
      text = it match {
        case None => anyText
        case Some(v) => v.toString
      }
    }
  }
}
