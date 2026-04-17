package fdswarm.scoring

import jakarta.inject.*
import scalafx.geometry.Insets
import scalafx.scene.control.TextArea
import scalafx.scene.layout.VBox

@Singleton
class ContestScoreResultsPane @Inject() (
                                          contestScoringService: ContestScoringService
                                        ):

  private val textArea = new TextArea:
    editable = false
    wrapText = false
    prefRowCount = 20
    prefColumnCount = 48

  private val rootPane = new VBox:
    padding = Insets(12)
    children = Seq(textArea)

  def pane: VBox =
    rootPane

  def refresh(): Unit =
    val result = contestScoringService.current

    val byModeLines =
      if result.byMode.isEmpty then Seq("  (none)")
      else result.byMode.toSeq.sortBy(_._1).map { case (k, v) => s"  $k: $v" }

    val byBandLines =
      if result.byBand.isEmpty then Seq("  (none)")
      else result.byBand.toSeq.sortBy(_._1).map { case (k, v) => s"  $k: $v" }

    val lines =
      Seq(
        s"Total Score : ${result.totalScore}",
        s"Raw Points  : ${result.rawPoints}",
        f"Multiplier  : ${result.multiplier}%.2f",
        s"Total QSOs  : ${result.totalQsos}",
        "",
        "By Mode:"
      ) ++
        byModeLines ++
        Seq("", "By Band:") ++
        byBandLines

    textArea.text = lines.mkString("\n")