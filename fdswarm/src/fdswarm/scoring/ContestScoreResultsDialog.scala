package fdswarm.scoring

import fdswarm.fx.FdLogUi
import jakarta.inject.*
import scalafx.Includes.jfxDialogPane2sfx
import scalafx.scene.control.{ButtonType, Dialog}

@Singleton
class ContestScoreResultsDialog @Inject() (
                                            resultsPane: ContestScoreResultsPane
                                          ):

  def show(): Unit =
    resultsPane.refresh()

    val dialog = new Dialog[ButtonType]:
      title = "Current Contest Score"
      headerText = "Live scoring values"
      dialogPane().buttonTypes = Seq(ButtonType.Close)
      dialogPane().content = resultsPane.pane
      initOwner(FdLogUi.primaryStage)

    dialog.showAndWait()