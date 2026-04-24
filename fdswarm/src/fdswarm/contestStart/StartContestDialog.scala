package fdswarm.contestStart

import jakarta.inject.{Inject, Singleton}
import scalafx.scene.control.{Alert, ButtonType}
import scalafx.stage.Window

import java.time.Instant

@Singleton
final class StartContestDialog @Inject()(
  contestStartManager: ContestStartManager
):

  def showStartContestDialog(
    ownerWindow: Window
  ): Unit =
    val currentContestStart = contestStartManager.contestStart.value.start
    val contestStartLabel =
      if currentContestStart == Instant.EPOCH then "Not Started"
      else currentContestStart.toString

    val dialog = new Alert(Alert.AlertType.Confirmation):
      initOwner(ownerWindow)
      title = "Start Contest"
      headerText = "Start Contest"
      contentText =
        s"Current contest start: $contestStartLabel\nThis will remove any QSOs from all nodes."
      buttonTypes = Seq(ButtonType.OK, ButtonType.Cancel)

    val result = dialog.showAndWait()
    if result.contains(ButtonType.OK) then
      showStartContestConfirmation(
        ownerWindow
      )

  private def showStartContestConfirmation(
    ownerWindow: Window
  ): Unit =
    val confirmationDialog = new Alert(Alert.AlertType.Warning):
      initOwner(ownerWindow)
      title = "Start Contest"
      headerText = "Start Contest"
      contentText = "All QSOs will be removed from all nodes."
    val result = confirmationDialog.showAndWait()
    if result.contains(ButtonType.OK) then
      contestStartManager.startContest()
