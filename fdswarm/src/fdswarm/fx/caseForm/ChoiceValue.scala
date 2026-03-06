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

import fdswarm.fx.SfxUtils.*
import scalafx.collections.ObservableBuffer
import scalafx.scene.control.{ComboBox, ListCell, ListView}

/** Something with a human-friendly label. */
trait HasName:
  def name: String

/**
 * Field wrapper: has a current value and a finite set of allowed choices.
 * (This is the thing you put in your case class, similar to how an enum field works.)
 */
final case class ChoiceValue[A <: AnyRef](
                                           value: A,
                                           choices: IndexedSeq[A],
                                           label: A => String
                                         ):
  /** Convenience if A extends HasName */
  def this(value: A, choices: IndexedSeq[A])(using ev: A <:< HasName) =
    this(value, choices, (a: A) => ev(a).name)

  /** Build a ScalaFX ComboBox for this choice set, with the current value selected. */
  def comboBox(): ComboBox[A] =
    val cb = new ComboBox[A](ObservableBuffer.from(choices))
    cb.value.value = value

    // Render the drop-down and the selected item using label(...)
    cb.cellFactory = (_: ListView[A]) =>
      new ListCell[A]:
        item.onChange { (_, _, it) =>
          text = if it == null then "" else label(it)
        }

    cb.buttonCell = new ListCell[A]:
      item.onChange { (_, _, it) =>
        text = if it == null then "" else label(it)
      }

    cb

    /** Same wrapper but with a new current selection. */
  def withValue(v: A): ChoiceValue[A] = copy(value = v)