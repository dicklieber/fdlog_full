package fdswarm.fx.discovery

import com.typesafe.scalalogging.LazyLogging
import fdswarm.fx.contest.{ContestConfig, ContestConfigManager, ContestConfigPane, ContestConfigPaneProvider, ContestType, ExchangePane}
import fdswarm.fx.utils.{GridColumn, GridColumnAlignment, GridColumnWidth, GridRowBehavior, RadioGroup, RadioGroupBuilder, StyledDialog, TypedGridTableBuilder}
import fdswarm.store.QsoStore
import jakarta.inject.Inject
import scalafx.Includes.*
import scalafx.application.Platform
import scalafx.scene.Node
import scalafx.scene.control.{Alert, Button, ButtonType, Dialog, ScrollPane}
import scalafx.scene.layout.{BorderPane, HBox, Region, VBox}
import scalafx.beans.property.ObjectProperty
import scalafx.scene.control.Alert.AlertType.{Confirmation, Error, Warning}

import scala.collection.mutable.ArrayBuffer

class DiscoveryDialog @Inject() (contestDiscovery: ContestDiscovery,
                                 contestConfigPaneProvider: ContestConfigPaneProvider,
                                 contestManager: ContestConfigManager,
                                 qsoStore: QsoStore,
                                 exchangePane: ExchangePane)
  extends StyledDialog[ButtonType] with LazyLogging:


  
  private val discovered = ArrayBuffer.empty[NodeContestStation]

//  private val contestConfig: ContestConfig = contestManager.contestConfig
  private val contestConfigPane: ContestConfigPane = contestConfigPaneProvider.pane()
  private val discoveryTable = new DiscoveryTable(contestConfigPane)




  val vBox = new VBox()
  val configBorderPane: BorderPane = new BorderPane {
    center = new VBox(spacing = 8) {
      children ++= Seq(
        contestConfigPane.pane,
        exchangePane.pane(contestConfigPane.currentContestConfigProperty)
      )
    }
    bottom = new Button("Update"):
      onAction = _ =>
        if qsoStore.hasQsos then
          new Alert(Error, "You already have QSOs logged!", ButtonType.OK, ButtonType.Cancel) {
            title = "Update Contest Configuration"
            headerText = "You already have QSOs logged!"
            buttonTypes = Seq(ButtonType.OK, ButtonType.Cancel)
            contentText =
                """You already have QSOs logged!
                  |Changing contest configuration during the contest is bad.
                  |Are you sure you want to continue?
                  |""".stripMargin

          }.showAndWait() match
            case Some(ButtonType.OK) =>
              val updatedContestConfig = contestConfigPane.finish()
              contestManager.setConfig(updatedContestConfig)
            case _ =>

  }
  vBox.children += configBorderPane
  vBox.children += discoveryTable.grid

  title = "Contest Configuration"
  resizable = true
  dialogPane().content = vBox
  dialogPane().buttonTypes = Seq(ButtonType.Close)
  onShowing.value = { (_: javafx.scene.control.DialogEvent) => Platform.runLater {
    val btn = dialogPane().lookupButton(ButtonType.Close)
    if (btn != null) {
      btn.setVisible(false)
    }
  } }

  private def refreshGrid(): Unit =
    discoveryTable.setItems(discovered)

  refreshGrid()

  contestDiscovery.discoverContest { ncs =>
    logger.info(s"Discovery UI added: $ncs")
    Platform.runLater {
      discovered += ncs
      refreshGrid()
    }
  }