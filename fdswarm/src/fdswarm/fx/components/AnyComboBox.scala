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

import fdswarm.model.Choice
import scalafx.collections.ObservableBuffer
import scalafx.scene.control.{ComboBox, ListCell, ListView}
import scalafx.scene.layout.Region
import scalafx.util.StringConverter

object AnyComboBox:
  def formatValue[T](
                      opt: Option[T],
                      anyText: String
                    ): String = opt match
    case null => ""
    case None => anyText
    case Some(v) => v.toString

  def parseValue[T](
                     s: String,
                     anyText: String,
                     choices: Seq[Choice[T]]
                   ): Option[T] =
    if s == null then None
    else if s == anyText then None
    else choices.find(_.value.toString == s).map(_.value)

/**
 * A ComboBox that includes an "-any-" option as None, and formats other options as "value - label".
 * It is sized based on the rendered value (T or "-any-"), while the dropdown can be wider.
 *
 * @param initialChoices A vararg of Selectable[T] instances.
 * @tparam T The type of the value.
 */
class AnyComboBox[T](initialChoices: Seq[Choice[T]]) extends ComboBox[Option[T]] {

  private val anyText = "-any-"
  private var mapInternal = Map.empty[Option[T], String]
  private var choicesInternal = Seq.empty[Choice[T]]

  def setChoices(newChoices: Choice[T]*): Unit = {
    choicesInternal = newChoices.toSeq
    val data = (None -> anyText) +: choicesInternal.map { s => Some(s.value) -> s.label }
    mapInternal = data.toMap
    items = ObservableBuffer.from(data.map(_._1))
  }

  setChoices(initialChoices*)
  value = None

  // The requirement says: "The combox as rendered should be sized based on the T or -any-
  // not the width of the choices. Choices should be as wide as needed, when comboBox is open"

  // cellFactory is used for rendering the items in the dropdown.
  cellFactory = (lv: ListView[Option[T]]) =>
    new ListCell[Option[T]] {
      item.onChange { (_, _, it) =>
        text = if (it == null) "" else mapInternal.getOrElse(it, "")
      }
    }

  converter = new StringConverter[Option[T]] {
    override def toString(opt: Option[T]): String =
      AnyComboBox.formatValue(
        opt,
        anyText
      )

    override def fromString(s: String): Option[T] =
      AnyComboBox.parseValue(
        s,
        anyText,
        choicesInternal
      )
  }

  // buttonCell is used for rendering the ComboBox itself (the button part).
  // JavaFX ComboBox uses the items + buttonCell/converter to compute its preferred width.
  // By using a buttonCell that only displays the short text, and having a converter
  // that also returns the short text, the ComboBox will size itself to the widest
  // of these short strings, satisfying the requirement.
  buttonCell = new ListCell[Option[T]] {
    item.onChange { (_, _, it) =>
      text = it match {
        case null => ""
        case None => anyText
        case Some(v) => v.toString
      }
    }
  }

  // JavaFX ComboBox's default sizing logic (ComboBoxListViewSkin) often iterates
  // through all items using the cellFactory to determine the preferred width.
  // To avoid sizing the ComboBox based on the long labels used in the dropdown,
  // we set its preferred width to be calculated based on the short values.
  prefWidth <== scalafx.beans.binding.Bindings.createDoubleBinding(
    () => {
      val strings = anyText +: choicesInternal.map(_.value.toString)
      val textObj = new scalafx.scene.text.Text()
      val maxW = strings.map { s =>
        textObj.text = s
        textObj.getLayoutBounds.getWidth
      }.maxOption.getOrElse(0.0)
      // Add padding for the ComboBox arrow and internal cell padding.
      // A typical arrow button + padding is around 20-30 pixels.
      // Increased to 60.0 to ensure "-any-" and "T" never show "..."
      maxW + 60.0
    },
    items
  )

  maxWidth = Region.USE_PREF_SIZE
  minWidth = Region.USE_PREF_SIZE
}
