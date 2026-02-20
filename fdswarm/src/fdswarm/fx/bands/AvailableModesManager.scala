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

package fdswarm.fx.bands

import fdswarm.io.DirectoryProvider
import fdswarm.model.BandMode.Mode
import jakarta.inject.{Inject, Singleton}
import scalafx.collections.ObservableBuffer
import javafx.collections.ListChangeListener
import io.circe.parser.*
import io.circe.syntax.*

@Singleton
final class AvailableModesManager @Inject()(
                                             dirProvider: DirectoryProvider
                                           ):
  private val path: os.Path =
    dirProvider() / "modes.json"

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
    val json = modes.toSeq.asJson.spaces2
    os.write.over(path, json, createFolders = true)

  private def loadFromDisk(): Seq[Mode] =
    try
      decode[Seq[Mode]](os.read(path)).toTry.get
    catch
      case _: Throwable =>
        Seq("PH")