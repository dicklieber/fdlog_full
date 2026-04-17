package fdswarm.scoring

import fdswarm.fx.FdLogUi
import fdswarm.fx.contest.ContestScoringConfigPane
import jakarta.inject.*
import scalafx.Includes.*
import scalafx.scene.control.{ButtonBar, ButtonType, Dialog}

@Singleton
class ContestScoringConfigDialog @Inject() (
                                             scoringPane: ContestScoringConfigPane
                                           ):

  def show(): Unit =
    scoringPane.reloadFromManager()
    val saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OKDone)

    val dialog = new Dialog[ButtonType]:
      title = "Contest Scoring"
      headerText = "Edit live contest scoring settings"
      dialogPane().buttonTypes = Seq(ButtonType.Cancel, saveButtonType)
      dialogPane().content = scoringPane.pane
      if FdLogUi.primaryStage != null then initOwner(FdLogUi.primaryStage)
      resultConverter = (buttonType: ButtonType) => buttonType

    val saveButton = dialog.dialogPane().lookupButton(saveButtonType)
    saveButton.disable <== scoringPane.saveDisabledBinding

    val result = dialog.delegate.showAndWait()
    if result.isPresent && result.get == saveButtonType then
      scoringPane.saveFromUi()
    else
      scoringPane.reloadFromManager()
