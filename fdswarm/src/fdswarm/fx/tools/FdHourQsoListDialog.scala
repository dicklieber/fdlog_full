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

import fdswarm.fx.qso.{FdHour, QsoDialog}
import fdswarm.model.Qso
import fdswarm.store.QsoStore
import scalafx.Includes.*
import scalafx.collections.ObservableBuffer
import scalafx.scene.Scene
import scalafx.scene.control.*
import scalafx.scene.layout.VBox
import scalafx.stage.{Stage, Window}

import java.time.format.DateTimeFormatter
import java.time.{Instant, ZoneId}

class FdHourQsoListDialog(qsoStore: QsoStore, ownerWindow: Window):
  private var currentFdHour: Option[FdHour] = None
  private val qsoBuffer = ObservableBuffer[Qso]()
  
  private val timeFmt =
    DateTimeFormatter.ofPattern("MMM dd, h:mm a z")
      .withZone(ZoneId.systemDefault())

  private def fmtInstant(i: Instant): String =
    timeFmt.format(i)

  private val table = new TableView[Qso](qsoBuffer):
    columnResizePolicy = javafx.scene.control.TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN
    rowFactory = { _ =>
      val row = new TableRow[Qso]()
      row.item.onChange { (_, _, qso) =>
        if qso != null then
          row.tooltip = Tooltip(qso.toString)
        else
          row.tooltip = null
      }
      row
    }
    columns ++= List(
      new TableColumn[Qso, String]("Time") {
        cellValueFactory = c => scalafx.beans.property.StringProperty(fmtInstant(c.value.stamp))
        prefWidth = 150
      },
      new TableColumn[Qso, String]("Their Call") {
        cellValueFactory = c => scalafx.beans.property.StringProperty(c.value.callsign.value)
        prefWidth = 100
      },
      new TableColumn[Qso, String]("Band") {
        cellValueFactory = c => scalafx.beans.property.StringProperty(c.value.bandMode.band)
        prefWidth = 60
      },
      new TableColumn[Qso, String]("Mode") {
        cellValueFactory = c => scalafx.beans.property.StringProperty(c.value.bandMode.mode)
        prefWidth = 60
      },
      new TableColumn[Qso, String]("Rcvd") {
        cellValueFactory = c => scalafx.beans.property.StringProperty(s"${c.value.contestClass} ${c.value.section}".trim)
        prefWidth = 100
      }
    )
    onMouseClicked = e =>
      if e.clickCount == 2 then
        val sel = selectionModel().getSelectedItem
        if sel != null then
          QsoDialog.show(sel)

  private val stage = new Stage():
    title = "QSOs for Hour"
    initOwner(ownerWindow)
    scene = new Scene:
      root = new VBox {
        children = Seq(
          new Label("QSOs for selected hour:"),
          table
        )
      }

  def showHour(fdHour: FdHour): Unit =
    currentFdHour = Some(fdHour)
    stage.title = s"QSOs for Hour: ${fdHour.display}"
    updateList()
    if !stage.showing() then
      // Position the dialog 1/3 the width of the parent to the left
      val w = ownerWindow.width.value / 3.0
      stage.width = w
      stage.height = ownerWindow.height.value
      stage.x = ownerWindow.x.value - w + 60
      stage.y = ownerWindow.y.value
      stage.show()
    else
      stage.toFront()

  private def updateList(): Unit =
    currentFdHour.foreach { hour =>
      val qsos = qsoStore.all.filter(_.fdHour == hour)
      qsoBuffer.setAll(qsos*)
    }
