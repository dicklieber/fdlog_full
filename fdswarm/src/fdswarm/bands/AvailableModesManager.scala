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

package fdswarm.bands

import fdswarm.io.FileHelper
import fdswarm.model.Mode
import jakarta.inject.{Inject, Singleton}
import scalafx.collections.ObservableBuffer
import javafx.collections.ListChangeListener

@Singleton
final class AvailableModesManager @Inject()(fileHelper: FileHelper):
 private val file = "modes.json"

  /** Single source of truth (ScalaFX-friendly) */
  val modes: ObservableBuffer[Mode] =
    ObservableBuffer.from(loadFromDisk())

  // Persist automatically when the buffer changes (same pattern as AvailableBandsManager)
  modes.delegate.addListener(
    new ListChangeListener[Mode]:
      override def onChanged(c: ListChangeListener.Change[? <: Mode]): Unit =
        persist()
  )

  /** Replace everything in the buffer from a Seq[Mode] */
  def setModes(newModes: Seq[Mode]): Unit =
    modes.setAll(newModes*)

  // ---- persistence ------------------------------------------------------------

  private def persist(): Unit =
    fileHelper.save(file, modes)

  private def loadFromDisk(): Seq[Mode] =
    fileHelper.loadOrDefault(file)(Seq(Mode.PH))
