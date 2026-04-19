package fdswarm.fx.discovery

import fdswarm.fx.contest.{
  ContestConfigManager,
  ExchangePane
}
import fdswarm.store.QsoStore
import scalafx.beans.property.BooleanProperty
import scalafx.scene.control.Alert.AlertType.Error
import scalafx.scene.control.{Alert, ButtonType}
import scalafx.scene.layout.{BorderPane, VBox}

class ContestDetailEditor(
  contestConfigPane: ContestConfigPane,
  exchangePane: ExchangePane,
  qsoStore: QsoStore,
  contestManager: ContestConfigManager
) extends BorderPane:

  val isValid: BooleanProperty = contestConfigPane.isValid

  center = new VBox(spacing = 8) {
    children ++= Seq(
      contestConfigPane.pane,
      exchangePane.pane(contestConfigPane.currentContestConfigProperty)
    )
  }

  def updateContestConfig(): Boolean =
    var continue = true
    if qsoStore.hasQsos then
      new Alert(
        Error,
        "You already have QSOs logged!",
        ButtonType.OK,
        ButtonType.Cancel
      ) {
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
      new Alert(
        Error,
        "This will delete all QSOs!",
        ButtonType.OK,
        ButtonType.Cancel
      ) {
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
    continue
