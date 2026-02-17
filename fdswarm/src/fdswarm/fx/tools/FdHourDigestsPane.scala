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

package fdswarm.fx.tools

import fdswarm.store.{FdHourDigest, QsoStore}
import jakarta.inject.{Inject, Singleton}
import scalafx.Includes.*
import scalafx.beans.property.ReadOnlyStringWrapper
import scalafx.collections.ObservableBuffer
import scalafx.scene.control.*
import scalafx.scene.layout.VBox
import scalafx.stage.Window

@Singleton
class FdHourDigestsPane @Inject()(qsoStore: QsoStore):
  
  private val table = new TableView[FdHourDigest]():
    columns ++= List(
      new TableColumn[FdHourDigest, String] {
        text = "Hour"
        cellValueFactory = { cellData => ReadOnlyStringWrapper(cellData.value.fdHour.display) }
        prefWidth = 100
      },
      new TableColumn[FdHourDigest, String] {
        text = "Count"
        cellValueFactory = { cellData => ReadOnlyStringWrapper(cellData.value.count.toString) }
        prefWidth = 80
      },
      new TableColumn[FdHourDigest, String] {
        text = "Digest"
        cellValueFactory = { cellData => ReadOnlyStringWrapper(cellData.value.digest) }
        prefWidth = 300
      }
    )

  def show(ownerWindow: Window): Unit =
    val digests = qsoStore.digests()
    table.items = ObservableBuffer.from(digests)
    
    val dialog = new Dialog[Unit]() {
      initOwner(ownerWindow)
      title = "FD Hour Digests"
      headerText = "Current QsoState.fdHourDigests"
    }

    val dialogPane = dialog.dialogPane.value
    dialogPane.content = new VBox {
      children = Seq(table)
    }
    dialogPane.buttonTypes = Seq(ButtonType.OK)
    
    dialog.showAndWait()
