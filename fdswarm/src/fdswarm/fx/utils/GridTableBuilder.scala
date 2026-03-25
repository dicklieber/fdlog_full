package fdswarm.fx.utils

import scalafx.Includes.*
import scalafx.geometry.Insets
import scalafx.geometry.Pos
import scalafx.scene.Node
import scalafx.scene.control.Label
import scalafx.scene.layout.*

class GridHeaderLabel(text0: String = "") extends Label(text0):
  styleClass += "grid-header-label"

object GridHeaderLabel:
  def apply(text: String): GridHeaderLabel = new GridHeaderLabel(text)

class GridTableCell(content: Node, styleClassName: String) extends StackPane:
  styleClass += "grid-table-cell"
  styleClass += styleClassName
  alignment = Pos.CenterLeft
  padding = Insets(6, 8, 6, 8)
  children = Seq(content)
  maxWidth = Double.MaxValue

object GridTableCell:
  def header(n: Node) = new GridTableCell(n, "grid-header-cell")
  def body(n: Node) = new GridTableCell(n, "grid-body-cell")

class GridTableBuilder(val grid: GridPane):
  private var row = 0

  def addHeaders(headers: String*): GridTableBuilder =
    headers.zipWithIndex.foreach((headerText, column) =>
      val cell = GridTableCell.header(GridHeaderLabel(headerText))
      GridPane.setHgrow(cell, Priority.Always)
      grid.add(cell, column, row)
    )
    row += 1
    this

  def addRow(values: String*): GridTableBuilder =
    values.zipWithIndex.foreach((v, c) =>
      val cell = GridTableCell.body(new Label(v))
      GridPane.setHgrow(cell, Priority.Always)
      grid.add(cell, c, row)
    )
    row += 1
    this

object GridTableBuilder:
  def apply(): GridTableBuilder =
    val g = new GridPane()
    g.gridLinesVisible = true
    g.styleClass += "grid-table"
    new GridTableBuilder(g)
