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
import scalafx.scene.input.KeyCode

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
    combo.focusTraversable = true

    var typedPrefix = ""
    var lastTypedAtMs = 0L
    val typedResetMs = 900L

    def normalized(
      value: T
    ): String =
      Option(value).map(buttonText).getOrElse("").trim.toLowerCase

    def advanceSelection(
      delta: Int
    ): Unit =
      val currentItems = combo.items.value
      if currentItems.nonEmpty then
        val currentIndex = combo.selectionModel().getSelectedIndex
        val start =
          if currentIndex >= 0 then currentIndex
          else if delta > 0 then -1
          else currentItems.size
        val nextIndex = (start + delta + currentItems.size) % currentItems.size
        combo.selectionModel().select(nextIndex)

    def selectByTypedPrefix(
      prefixRaw: String
    ): Unit =
      val prefix = prefixRaw.trim.toLowerCase
      if prefix.nonEmpty then
        combo.items.value.indexWhere(item => normalized(item).startsWith(prefix)) match
          case index if index >= 0 =>
            combo.selectionModel().select(index)
          case _ =>
            ()

    combo.onKeyPressed = event =>
      event.code match
        case KeyCode.Up =>
          advanceSelection(-1)
          event.consume()
        case KeyCode.Down =>
          advanceSelection(1)
          event.consume()
        case _ =>
          ()

    combo.onKeyTyped = event =>
      val now = System.currentTimeMillis()
      if now - lastTypedAtMs > typedResetMs then
        typedPrefix = ""
      lastTypedAtMs = now

      Option(event.character).map(_.trim).filter(_.nonEmpty) match
        case Some(chars) =>
          typedPrefix = s"$typedPrefix$chars"
          selectByTypedPrefix(typedPrefix)
        case None =>
          ()

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
