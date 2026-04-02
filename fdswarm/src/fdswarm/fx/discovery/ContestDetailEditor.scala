package fdswarm.fx.discovery

import fdswarm.fx.contest.{
  ContestConfigManager,
  ContestConfigPane,
  ExchangePane
}
import fdswarm.store.QsoStore
import scalafx.scene.control.Alert.AlertType.Error
import scalafx.scene.control.{Alert, Button, ButtonType, TitledPane}
import scalafx.scene.layout.{BorderPane, VBox}
import javafx.stage.{Stage as JStage}

class ContestDetailEditor(contestConfigPane: ContestConfigPane,
                          exchangePane: ExchangePane,
                          qsoStore: QsoStore,
                          contestManager: ContestConfigManager) extends TitledPane:


  private val borderPane = new BorderPane()
  borderPane.center = new VBox(spacing = 8) {
    children ++= Seq(
      contestConfigPane.pane,
      exchangePane.pane(contestConfigPane.currentContestConfigProperty)
    )
  }
  borderPane.bottom = new Button("Update"):
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
          // continue = true
          case None =>
            continue = false
        }
        if continue then
          val updatedContestConfig = contestConfigPane.finish()
          contestManager.setConfig(updatedContestConfig)
          val currentScene = borderPane.scene.value
          if currentScene != null then
            val window = currentScene.getWindow
            if window != null then
              window match
                case stage: JStage => stage.close()
                case _ => window.hide()

        // todo
  text = "Contest Details"
  content = borderPane
  collapsible = false
