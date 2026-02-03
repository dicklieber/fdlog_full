package fdswarm.fx.qso

import fdswarm.model.Qso
import fdswarm.store.QsoStore
import jakarta.inject.*
import scalafx.collections.ObservableBuffer
import scalafx.scene.Node
import scalafx.scene.control.*

import java.time.format.DateTimeFormatter
import java.time.{Instant, ZoneId}

/**
 * Table of QSOs.
 * @param qsoStore where qsos live [[QsoStore.qsoCollection]]
 */
@Singleton
class QsoTablePane @Inject(qsoStore:QsoStore):
  private val qsoCollection: ObservableBuffer[Qso] = qsoStore.qsoCollection

  private val timeFmt =
    DateTimeFormatter.ofPattern("HH:mm:ss")
      .withZone(ZoneId.systemDefault())

  private def fmtInstant(i: Instant): String =
    timeFmt.format(i)

  private def fmtFreqHz(hz: Long): String =
    // show kHz with 1 decimal if you like; tweak as desired
    f"${hz.toDouble / 1000.0}%.1f kHz"

  private val table = new TableView[Qso](qsoCollection):
    columnResizePolicy = javafx.scene.control.TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN
    placeholder = new Label("No QSOs yet")

  private def col[S](title: String, value: Qso => S): TableColumn[Qso, S] =
    new TableColumn[Qso, S](title):
      cellValueFactory = c => scalafx.beans.property.ObjectProperty(value(c.value))

  // --- columns ---------------------------------------------------------------

  private val timeCol = col[String]("Time", q => fmtInstant(q.stamp))
  private val bandCol = col[String]("Band", _.bandMode.bandName)
  private val modeCol = col[String]("Mode", _.bandMode.modeName)
  private val theirCallCol = col[String]("Their Call", _.callSign.value)
  private val rcvdCol   = col[String]("Rcvd", q => s"${q.contestClass} ${q.section}".trim)
  private val opCol = col[String]("Op", _.qsoMetadata.station.operator.value)

  table.columns ++= Seq(
    timeCol, theirCallCol, bandCol, modeCol,  rcvdCol, opCol
  )

  // Optional: make “Their” callsign stand out a bit (still text-only)
  theirCallCol.styleClass += "qso-their-call"

  // Optional: double click row to do something
/*
  table.onMouseClicked = e =>
    if e.clickCount == 2 then
      val sel = table.selectionModel().getSelectedItem
      if sel != null then
        // hook your own behavior here
        println(s"Double-click: ${sel.theirCall} on ${sel.band} ${sel.mode}")
*/


  private val countLabel = new Label:
    text <== scalafx.beans.binding.Bindings.createStringBinding(
      () => s"${qsoCollection.size} QSOs",
      qsoCollection
    )

  val node: Node = new TitledPane() {
//    text = "QSOs"
    collapsible = false
    graphic = countLabel
    content = table
  }