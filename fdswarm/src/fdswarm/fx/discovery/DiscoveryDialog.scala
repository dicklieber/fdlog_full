package fdswarm.fx.discovery

import com.typesafe.scalalogging.LazyLogging
import fdswarm.fx.contest.{
  ContestConfigManager,
  ContestConfigPane,
  ContestConfigPaneProvider,
  ExchangePane
}
import fdswarm.fx.utils.StyledDialog
import fdswarm.store.QsoStore
import jakarta.inject.Inject
import scalafx.Includes.*
import scalafx.application.Platform
import scalafx.scene.control.Alert.AlertType.Error
import scalafx.scene.control.{Alert, Button, ButtonType}
import scalafx.scene.layout.{BorderPane, VBox}

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
        var continue = true
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
            case _ =>
              continue = false
        if continue then
          new Alert(Error, "This will delete all QSOs!", ButtonType.OK, ButtonType.Cancel) {
            title = "Start Contest"
            headerText = "This will delete all QSOs!"
            buttonTypes = Seq(ButtonType.OK, ButtonType.Cancel)
            contentText =
              """For all nodes this will delete all QSOs logged so far and set the new Contest Configuration.!
                |Are you sure you want to continue?
                |""".stripMargin
          }.showAndWait() match {
            case Some(value) =>
//              continue = true
            case None =>
              continue = false
          }
          val updatedContestConfig = contestConfigPane.finish()
          contestManager.setConfig(updatedContestConfig)
          // todo


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