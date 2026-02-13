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
import jakarta.inject.{Inject, Singleton}
import upickle.default.*
import fdswarm.model.BandMode.Band

import scalafx.collections.ObservableBuffer
import javafx.collections.ListChangeListener

@Singleton
final class AvailableBandsManager @Inject()(
                                             dirProvider: DirectoryProvider
                                           ):
  private val path: os.Path =
    dirProvider() / "bands.json"

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
    val json = write(bands.toSeq, indent = 2)
    os.write.over(path, json, createFolders = true)

  private def loadFromDisk(): Seq[Band] =
    try
      read[Seq[Band]](os.read(path))
    catch
      case _: Throwable =>
        Seq("20m")