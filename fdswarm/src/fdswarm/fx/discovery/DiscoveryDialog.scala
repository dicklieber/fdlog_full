package fdswarm.fx.discovery

import com.typesafe.scalalogging.LazyLogging
import fdswarm.fx.contest.{ContestConfig, ContestConfigManager, ContestConfigPaneProvider, ContestType, ExchangePane}
import fdswarm.fx.utils.{GridColumn, GridColumnAlignment, GridColumnWidth, GridRowBehavior, RadioGroup, RadioGroupBuilder, StyledDialog, TypedGridTableBuilder}
import jakarta.inject.Inject
import scalafx.Includes.*
import scalafx.application.Platform
import scalafx.scene.Node
import scalafx.scene.control.Button
import scalafx.scene.control.ButtonType
import scalafx.scene.control.Dialog
import scalafx.scene.control.ScrollPane
import scalafx.scene.layout.{BorderPane, HBox, Region, VBox}
import scalafx.beans.property.ObjectProperty

import scala.collection.mutable.ArrayBuffer

class DiscoveryDialog @Inject() (contestDiscovery: ContestDiscovery,
                                 contestConfigPaneProvider: ContestConfigPaneProvider,
                                 contestManager: ContestConfigManager,
                                 exchangePane: ExchangePane)
  extends StyledDialog[ButtonType] with LazyLogging:


  
  private val discovered = ArrayBuffer.empty[NodeContestStation]

//  private val contestConfig: ContestConfig = contestManager.contestConfig
  private val contestConfigPane: contestConfigPaneProvider.ContestConfigPane = contestConfigPaneProvider.pane()
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
        val updatedContestConfig = contestConfigPane.finish()
        contestManager.setConfig(updatedContestConfig)
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