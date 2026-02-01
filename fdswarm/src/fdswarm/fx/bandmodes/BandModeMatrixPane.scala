package fdswarm.fx.bandmodes

import jakarta.inject.{Inject, Singleton}
import scalafx.Includes.*
import scalafx.beans.property.{BooleanProperty, ObjectProperty, StringProperty}
import scalafx.collections.ObservableBuffer
import scalafx.geometry.Insets
import scalafx.scene.control.*
import scalafx.scene.input.{KeyCode, KeyEvent}
import scalafx.scene.layout.BorderPane

import javafx.beans.value.ObservableValue
import javafx.util.Callback
import javafx.scene.control.ContentDisplay

@Singleton
final class BandModeMatrixPane @Inject() (
                                           store: BandModeStore,
                                           selectedStore: SelectedBandModeStore
                                         ) extends BorderPane {

  private val visibleModes: ObjectProperty[Seq[String]] = ObjectProperty(Seq.empty)
  private val visibleBands: ObjectProperty[Seq[String]] = ObjectProperty(Seq.empty)

  private val rows: ObservableBuffer[ModeRow] = ObservableBuffer.empty

  private val enabledProps =
    collection.mutable.Map.empty[(String, String), BooleanProperty]

  private val listened =
    collection.mutable.Set.empty[(String, String)]

  private val focused: ObjectProperty[Option[BandMode]] =
    ObjectProperty(selectedStore.current)

  selectedStore.selected.onChange { (_, _, nv) =>
    focused.value = nv
    table.refresh()
  }

  final class ModeRow(val mode: String):
    val modeName: StringProperty = StringProperty(mode)
    def enabledFor(band: String): BooleanProperty =
      enabledProps.getOrElseUpdate((mode, band), BooleanProperty(false))

  private val table: TableView[ModeRow] = new TableView[ModeRow](rows) {
    editable = true
    columnResizePolicy = TableView.ConstrainedResizePolicy
  }

  padding = Insets(10)
  center = table

  table.addEventFilter(KeyEvent.KeyPressed, (ke: KeyEvent) => handleKey(ke))

  def setVisible(modes: Seq[String], bands: Seq[String]): Unit = {
    visibleModes.value = modes.distinct
    visibleBands.value = bands.distinct
    rebuild()
  }

  def clearSelection(): Unit = {
    selectedStore.set(None)
    focused.value = None
    table.refresh()
  }

  def selectedBandMode: Option[BandMode] =
    selectedStore.current

  def refreshFromStore(): Unit = {
    val bm = store.currentBandMode
    for (m <- visibleModes.value; b <- visibleBands.value) {
      val p = enabledProps.getOrElseUpdate((m, b), BooleanProperty(false))
      p.value = bm.enabled.getOrElse(m, Set.empty).contains(b)
    }
    table.refresh()
  }

  private def rebuild(): Unit = {
    rebuildColumns()
    rebuildRows()
    refreshFromStore()
  }

  private def rebuildColumns(): Unit = {
    table.columns.clear()

    table.columns += new TableColumn[ModeRow, String]("Mode") {
      cellValueFactory = _.value.modeName
      editable = false
      prefWidth = 120
    }

    visibleBands.value.foreach { band =>
      val col = new TableColumn[ModeRow, java.lang.Boolean](band)

      col.delegate.setCellValueFactory(
        new Callback[javafx.scene.control.TableColumn.CellDataFeatures[ModeRow, java.lang.Boolean], ObservableValue[java.lang.Boolean]] {
          override def call(cdf: javafx.scene.control.TableColumn.CellDataFeatures[ModeRow, java.lang.Boolean]): ObservableValue[java.lang.Boolean] =
            cdf.getValue.enabledFor(band).delegate
        }
      )

      col.delegate.setCellFactory(
        new Callback[javafx.scene.control.TableColumn[ModeRow, java.lang.Boolean], javafx.scene.control.TableCell[ModeRow, java.lang.Boolean]] {
          override def call(tc: javafx.scene.control.TableColumn[ModeRow, java.lang.Boolean]) =
            new javafx.scene.control.TableCell[ModeRow, java.lang.Boolean]() {

              private val cb = new javafx.scene.control.CheckBox()
              private var boundTo: javafx.beans.property.BooleanProperty | Null = null

              setGraphic(cb)
              setContentDisplay(ContentDisplay.GRAPHIC_ONLY)

              cb.setOnAction(_ => {
                persistEnabledFromProps()
                getTableView.requestFocus()
                getTableView.refresh()
              })

              override def updateItem(item: java.lang.Boolean, empty: Boolean): Unit = {
                super.updateItem(item, empty)

                if (boundTo != null) cb.selectedProperty().unbindBidirectional(boundTo)
                boundTo = null

                val rowObjOpt =
                  Option(getTableRow).flatMap(r => Option(r.getItem))

                if (empty || rowObjOpt.isEmpty) {
                  setGraphic(null)
                  setTooltip(null)
                  setStyle("")
                } else {
                  val rowObj = rowObjOpt.get
                  val p = rowObj.enabledFor(band).delegate
                  boundTo = p
                  cb.selectedProperty().bindBidirectional(p)

                  setGraphic(cb)
                  setTooltip(new javafx.scene.control.Tooltip(s"${rowObj.mode} on $band"))

                  val isSel =
                    selectedStore.current.exists(s => s.mode == rowObj.mode && s.band == band)

                  setStyle(
                    if (isSel) "-fx-border-color: -fx-focus-color; -fx-border-width: 2; -fx-border-insets: 1;"
                    else ""
                  )
                }
              }

              setOnMousePressed(_ => {
                val rowObjOpt =
                  Option(getTableRow).flatMap(r => Option(r.getItem))

                rowObjOpt.foreach { r =>
                  val bm = BandMode(band = band, mode = r.mode)
                  selectedStore.set(Some(bm))
                  focused.value = Some(bm)
                  getTableView.requestFocus()
                  getTableView.refresh()
                }
              })
            }
        }
      )

      table.columns += col
    }
  }

  private def rebuildRows(): Unit = {
    rows.setAll(visibleModes.value.map(new ModeRow(_))*)
    attachListenersOnce()
  }

  private def attachListenersOnce(): Unit = {
    val modes = visibleModes.value
    val bands = visibleBands.value

    for (m <- modes; b <- bands)
      enabledProps.getOrElseUpdate((m, b), BooleanProperty(false))

    for (m <- modes; b <- bands) {
      val k = (m, b)
      if (!listened.contains(k)) {
        listened += k
        enabledProps(k).onChange { (_, _, _) =>
          persistEnabledFromProps()
        }
      }
    }
  }

  private def persistEnabledFromProps(): Unit = {
    val modes = visibleModes.value
    val bands = visibleBands.value

    val enabled: Map[String, Set[String]] =
      modes.map { m =>
        val bs = bands.collect { case b if enabledProps((m, b)).value => b }.toSet
        m -> bs
      }.toMap

    store.updateEnabledOnly(enabled)
  }

  private def handleKey(ke: KeyEvent): Unit = {
    val modes = visibleModes.value
    val bands = visibleBands.value
    if (modes.isEmpty || bands.isEmpty) return

    def idxMode(m: String): Int = modes.indexOf(m) match
      case -1 => 0
      case n  => n

    def idxBand(b: String): Int = bands.indexOf(b) match
      case -1 => 0
      case n  => n

    val cur = focused.value.orElse(selectedStore.current).getOrElse(BandMode(bands.head, modes.head))
    var mi = idxMode(cur.mode)
    var bi = idxBand(cur.band)

    ke.code match
      case KeyCode.Left  => bi = math.max(0, bi - 1); selectAt(mi, bi); ke.consume()
      case KeyCode.Right => bi = math.min(bands.size - 1, bi + 1); selectAt(mi, bi); ke.consume()
      case KeyCode.Up    => mi = math.max(0, mi - 1); selectAt(mi, bi); ke.consume()
      case KeyCode.Down  => mi = math.min(modes.size - 1, mi + 1); selectAt(mi, bi); ke.consume()
      case KeyCode.Space =>
        val m = modes(mi); val b = bands(bi)
        val p = enabledProps.getOrElseUpdate((m, b), BooleanProperty(false))
        p.value = !p.value
        persistEnabledFromProps()
        table.refresh()
        ke.consume()
      case KeyCode.Enter =>
        selectAt(mi, bi)
        ke.consume()
      case _ => ()
  }

  private def selectAt(mi: Int, bi: Int): Unit = {
    val bm = BandMode(band = visibleBands.value(bi), mode = visibleModes.value(mi))
    selectedStore.set(Some(bm))
    focused.value = Some(bm)
    table.refresh()
  }
}
