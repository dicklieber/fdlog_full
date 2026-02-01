package fdswarm.fx

import jakarta.inject.{Inject, Singleton}
import scalafx.scene.control.*
import scalafx.geometry.Insets
import scalafx.scene.layout.GridPane
import scalafx.Includes.*
import com.typesafe.scalalogging.LazyLogging

import fdswarm.store.QsoStore
import fdswarm.model.*
import fdswarm.fx.bandmodes.SelectedBandModeStore

@Singleton
class QsoEntryPanel @Inject()(
                               qsoStore: QsoStore,
                               selectedBandModeStore: SelectedBandModeStore
                             ) extends GridPane, LazyLogging:

  // ---- state -------------------------------------------------------------

  private val bandMode = BandMode() // TODO: wire from SelectedBandModeStore

  private val qsoMetadata =
    QsoMetadata(
      station = StationPersisted(
        bandName = "20m",
        mode     = "CW",
        rig      = "",
        antenna  = "",
        operator = Callsign("")
      ),
      node    = "local",
      contest = Contest.WFD
    )

  // ---- controls ----------------------------------------------------------

  private val callSignField     = new TextField()
  private val contestClassField = new TextField()
  private val sectionField      = new TextField()

  // ---- layout ------------------------------------------------------------

  hgap = 8
  vgap = 8
  padding = Insets(10)

  add(new Label("Call sign:"), 0, 0)
  add(callSignField,           1, 0)

  add(new Label("Class:"),     0, 1)
  add(contestClassField,       1, 1)

  add(new Label("Section:"),   0, 2)
  add(sectionField,            1, 2)

  // ---- behavior ----------------------------------------------------------

  sectionField.onAction = _ => submit()

  private def submit(): Unit =
    logger.debug(
      s"Submitting QSO: call=${callSignField.text.value}, " +
        s"class=${contestClassField.text.value}, " +
        s"section=${sectionField.text.value}"
    )

    val qso = Qso(
      callSignField.text.value,
      contestClassField.text.value,
      sectionField.text.value,
      bandMode,
      qsoMetadata
    )

    qsoStore.add(qso)
    callSignField.text.value = ""