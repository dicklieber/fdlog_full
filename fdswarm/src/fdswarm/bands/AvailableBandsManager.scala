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
import fdswarm.model.BandMode.Band
import jakarta.inject.{Inject, Singleton}
import javafx.collections.ListChangeListener
import scalafx.collections.ObservableBuffer

@Singleton
final class AvailableBandsManager @Inject()(fileHelper:FileHelper):
  private val file ="bands.json"

  /** Single source of truth */
  val bands: ObservableBuffer[Band] =
    ObservableBuffer.from(loadFromDisk())

  // Persist automatically when the buffer changes
  bands.delegate.addListener(
    new ListChangeListener[Band]:
      override def onChanged(c: ListChangeListener.Change[? <: Band]): Unit =
        persist()
  )

  // ---- persistence ------------------------------------------------------------

  private def persist(): Unit =
    fileHelper.save(file, bands)

  private def loadFromDisk(): Seq[Band] =
    fileHelper.loadOrDefault(file)(Seq("20m"))
