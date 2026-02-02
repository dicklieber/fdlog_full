package fdswarm.fx

import jakarta.inject.{Inject, Singleton}
import scalafx.scene.control.*
import scalafx.geometry.Insets
import scalafx.scene.layout.GridPane
import scalafx.Includes.*
import com.typesafe.scalalogging.LazyLogging
import fdswarm.StationManager
import fdswarm.store.QsoStore
import fdswarm.model.*
import fdswarm.fx.bandmodes.SelectedBandModeStore
import scalafx.scene.Node

@Singleton
class QsoEntryPanel @Inject()(
                               qsoStore: QsoStore,
                               selectedBandModeStore: SelectedBandModeStore,
                               stationManager: StationManager
                             ) extends LazyLogging:

  val callSignField = UpperCase(new TextField())
  val contestClassField = UpperCase(new TextField())
  val sectionField = UpperCase(new TextField())

  val node: Node =

    val grid = new GridPane {

      add(new Label("Their Callsign:"), 0, 0)
      add(callSignField, 0, 1)

      add(new Label("Received Class:"), 1, 0)
      add(contestClassField, 1, 1)

      add(new Label("Received Section:"), 2, 0)
      add(sectionField, 2, 1)
    }
    new TitledPane() {
      text = "QSO"
      collapsible = false
      content = grid
    }
  sectionField.onAction = _ =>
    submit()
  private val bandMode = BandMode() // TODO: wire from SelectedBandModeStore

  // ---- controls ----------------------------------------------------------
  private val qsoMetadata = //todo add a QsoMetadataStore
    QsoMetadata(
      station = stationManager.station,
      node = "local",
      contest = Contest.WFD
    )


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