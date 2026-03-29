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

package fdswarm.fx.contest

import scalafx.Includes.*
import scalafx.application.Platform
import scalafx.scene.control.{ComboBox, ListCell, ListView}

trait CompactComboBoxSupport:
  protected def configureCompactComboBox[T](
    combo: ComboBox[T],
    closedWidth: Double = 90.0,
    popupWidth: Double = 260.0
  )(
    buttonText: T => String,
    listText: T => String
  ): ComboBox[T] =
    combo.minWidth = closedWidth
    combo.prefWidth = closedWidth
    combo.maxWidth = closedWidth

    combo.cellFactory = (_: ListView[T]) => new ListCell[T]:
      item.onChange(
        (_, _, newValue) =>
          text = Option(newValue).map(listText).getOrElse("")
      )

    combo.buttonCell = new ListCell[T]:
      item.onChange(
        (_, _, newValue) =>
          text = Option(newValue).map(buttonText).getOrElse("")
      )

    combo.showing.onChange(
      (_, _, isShowing) =>
        if isShowing then
          Platform.runLater {
            val popupStyle = s"-fx-pref-width: ${popupWidth}px; -fx-min-width: ${popupWidth}px;"

            Option(combo.delegate.lookup(".list-view")).foreach(_.setStyle(popupStyle))
            Option(combo.delegate.lookup(".combo-box-popup .list-view")).foreach(_.setStyle(popupStyle))
          }
    )

    combo
