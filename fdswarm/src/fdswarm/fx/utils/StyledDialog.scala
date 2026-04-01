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

package fdswarm.fx.utils

import javafx.stage.{Modality, Window as JfxWindow}
import scalafx.scene.control.Dialog

/**
 * A dialog with UiStyles applied to it.
 *
 * By default it:
 *   - applies app.css via UiStyles
 *   - attaches itself to the currently showing window, if there is one
 *   - uses WindowModal when an owner window is found
 *
 * You can still call initOwner(...) or initModality(...) yourself in a
 * subclass if you need different behavior.
 *
 * @tparam R dialog result type
 */
abstract class StyledDialog[R] extends Dialog[R]:

  UiStyles.applyTo(dialogPane())

  StyledDialog.defaultOwner.foreach { owner =>
    initOwner(owner)
    initModality(Modality.WINDOW_MODAL)
  }

object StyledDialog:

  /**
   * Returns the front-most showing JavaFX window, if any.
   */
  def defaultOwner: Option[JfxWindow] =
    Option(JfxWindow.getWindows)
      .map(_.toArray.toSeq.collect { case w: JfxWindow if w.isShowing => w })
      .flatMap(_.lastOption)
