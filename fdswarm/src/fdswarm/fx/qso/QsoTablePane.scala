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

package fdswarm.fx.qso

import fdswarm.fx.{GridColumns, UserConfig}
import fdswarm.model.Qso
import fdswarm.store.QsoStore
import jakarta.inject.*
import scalafx.Includes.*
import scalafx.beans.property.IntegerProperty
import scalafx.beans.binding.Bindings
import scalafx.collections.ObservableBuffer
import scalafx.scene.Node
import scalafx.scene.control.*
import scalafx.scene.layout.VBox

import java.time.format.DateTimeFormatter
import java.time.{Instant, ZoneId}

/**
 * Table of QSOs.
 * @param qsoStore where qsos live [[QsoStore.qsoCollection]]
 */
@Singleton
class QsoTablePane @Inject()(qsoStore: QsoStore, userConfig: UserConfig, qsoSearchPane: QsoSearchPane):
  private val qsoCollection: ObservableBuffer[Qso] = qsoStore.qsoCollection

  private val filteredQsos = new javafx.collections.transformation.FilteredList[Qso](qsoCollection.delegate)

  private val filterProperty = scalafx.beans.property.ObjectProperty[Qso => Boolean](_ => true)
  qsoSearchPane.callsignFilter.text.onChange(filterProperty.value = qsoSearchPane.filter)
  qsoSearchPane.bandFilter.value.onChange(filterProperty.value = qsoSearchPane.filter)
  qsoSearchPane.modeFilter.value.onChange(filterProperty.value = qsoSearchPane.filter)
//  qsoSearchPane.classFilter.value.onChange(filterProperty.value = qsoSearchPane.filter)
  qsoSearchPane.operatorFilter.text.onChange(filterProperty.value = qsoSearchPane.filter)
  qsoSearchPane.expandedProperty.onChange(filterProperty.value = qsoSearchPane.filter)

  filterProperty.onChange { (_, _, f) =>
    filteredQsos.setPredicate(q => f(q))
  }

  qsoSearchPane.filteredQsosSupplier = () => {
    import scala.jdk.CollectionConverters.*
    filteredQsos.asScala.toSeq
  }

  private val timeFmt =
    DateTimeFormatter.ofPattern("MMM dd, h:mm a z")
      .withZone(ZoneId.systemDefault())

  private def fmtInstant(i: Instant): String =
    timeFmt.format(i)

  private def fmtFreqHz(hz: Long): String =
    // show kHz with 1 decimal if you like; tweak as desired
    f"${hz.toDouble / 1000.0}%.1f kHz"

  private val table = new TableView[Qso] {
    items = new scalafx.collections.ObservableBuffer(filteredQsos)
    columnResizePolicy = javafx.scene.control.TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN
    placeholder = new Label("No QSOs yet")
    prefHeight <== userConfig.getProperty[IntegerProperty]("qsoListLines") * 25 // Roughly 25 pixels per row
  }

  private def col[S](title: String, value: Qso => S): TableColumn[Qso, S] =
    new TableColumn[Qso, S](title):
      cellValueFactory = c => scalafx.beans.property.ObjectProperty(value(c.value))

  // --- columns ---------------------------------------------------------------

  private val timeCol = col[String]("Time", q => fmtInstant(q.stamp))
  private val bandCol = col[String]("Band", _.bandMode.band)
  private val modeCol = col[String]("Mode", _.bandMode.mode)
  private val theirCallCol = col[String]("Their Call", _.callsign.value)
  private val rcvdCol   = col[String]("Rcvd", q => q.exchange.toString)
  private val opCol = col[String]("Op", _.qsoMetadata.station.operator.value)

  table.columns ++= Seq(
    timeCol, theirCallCol, bandCol, modeCol,  rcvdCol, opCol
  )

  // Optional: make “Their” callsign stand out a bit (still text-only)
  theirCallCol.styleClass += "qso-their-call"

  // Optional: double click row to do something
  table.onMouseClicked = e =>
    if e.clickCount == 2 then
      val sel = table.selectionModel().getSelectedItem
      if sel != null then
        QsoDialog.show(sel)


  private val countLabel = new Label:
    text <== scalafx.beans.binding.Bindings.createStringBinding(
      () => f"${filteredQsos.size}%,d QSOs",
      qsoCollection,
      filterProperty
    )

  val node: Node =
    GridColumns.fieldSet("QSOs", new VBox {
      children = Seq(
        qsoSearchPane.node,
        countLabel,
        table
      )
    })