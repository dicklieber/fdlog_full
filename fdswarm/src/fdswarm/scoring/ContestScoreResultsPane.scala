package fdswarm.scoring

import jakarta.inject.*
import scalafx.geometry.Insets
import scalafx.scene.control.Label
import scalafx.geometry.Pos
import scalafx.scene.layout.{GridPane, Priority, VBox}

import java.text.NumberFormat
import java.util.Locale

@Singleton
class ContestScoreResultsPane @Inject() (
                                          contestScoringService: ContestScoringService
                                        ):

  private val intFormat = NumberFormat.getIntegerInstance(
    Locale.US
  )

  private val decimalFormat =
    val formatter = NumberFormat.getNumberInstance(
      Locale.US
    )
    formatter.setMinimumFractionDigits(
      2
    )
    formatter.setMaximumFractionDigits(
      2
    )
    formatter

  private val scoreGrid = new GridPane:
    hgap = 8
    vgap = 2

  private val rootPane = new VBox:
    padding = Insets(12)
    children = Seq(scoreGrid)

  def pane: VBox =
    rootPane

  private def formatInt(
                         value: Int
                       ): String =
    intFormat.format(
      value
    )

  private def formatDouble(
                            value: Double
                          ): String =
    decimalFormat.format(
      value
    )

  private def newSectionHeader(
                                text: String
                              ): Label =
    new Label(
      text
    ):
      styleClass += "grid-header"
      alignment = Pos.Center
      maxWidth = Double.MaxValue

  private def newNameCell(
                           text: String
                         ): Label =
    new Label(
      text
    ):
      styleClass += "grid-row-label"
      maxWidth = Double.MaxValue

  private def newValueCell(
                            text: String
                          ): Label =
    new Label(
      text
    ):
      styleClass += "grid-value"
      alignment = Pos.CenterRight
      maxWidth = Double.MaxValue

  private def addSectionHeaderRow(
                                   row: Int,
                                   sectionName: String
                                 ): Int =
    val sectionHeader = newSectionHeader(
      sectionName
    )
    GridPane.setHgrow(
      sectionHeader,
      Priority.Always
    )
    scoreGrid.add(
      sectionHeader,
      0,
      row,
      2,
      1
    )
    row + 1

  private def addNameValueRow(
                               row: Int,
                               name: String,
                               value: String
                             ): Int =
    val nameCell = newNameCell(
      name
    )
    GridPane.setHgrow(
      nameCell,
      Priority.Always
    )
    scoreGrid.add(
      nameCell,
      0,
      row
    )
    val valueCell = newValueCell(
      value
    )
    GridPane.setHgrow(
      valueCell,
      Priority.Always
    )
    scoreGrid.add(
      valueCell,
      1,
      row
    )
    row + 1

  def refresh(): Unit =
    val result = contestScoringService.current

    scoreGrid.children.clear()

    var row = 0

    row = addSectionHeaderRow(
      row = row,
      sectionName = "Totals"
    )
    row = addNameValueRow(
      row = row,
      name = "Total Score",
      value = formatInt(
        result.totalScore
      )
    )
    row = addNameValueRow(
      row = row,
      name = "Raw Points",
      value = formatInt(
        result.rawPoints
      )
    )
    row = addNameValueRow(
      row = row,
      name = "Multiplier",
      value = formatDouble(
        result.multiplier
      )
    )
    row = addNameValueRow(
      row = row,
      name = "Total QSOs",
      value = formatInt(
        result.totalQsos
      )
    )

    row = addSectionHeaderRow(
      row = row,
      sectionName = "By Mode"
    )
    if result.byMode.isEmpty then
      row = addNameValueRow(
        row = row,
        name = "(none)",
        value = ""
      )
    else
      result.byMode.toSeq.sortBy(_._1).foreach:
        case (mode, count) =>
          row = addNameValueRow(
            row = row,
            name = mode,
            value = formatInt(
              count
            )
          )

    row = addSectionHeaderRow(
      row = row,
      sectionName = "By Band"
    )
    if result.byBand.isEmpty then
      row = addNameValueRow(
        row = row,
        name = "(none)",
        value = ""
      )
    else
      result.byBand.toSeq.sortBy(_._1).foreach:
        case (band, count) =>
          row = addNameValueRow(
            row = row,
            name = band,
            value = formatInt(
              count
            )
          )
