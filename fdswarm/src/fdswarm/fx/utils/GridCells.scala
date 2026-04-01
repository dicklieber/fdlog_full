package fdswarm.fx.utils

import scalafx.scene.Node
import scalafx.scene.layout.{GridPane, StackPane}

object GridCells:
  def styledGrid(extraGridStyles: String*): GridPane =
    new GridPane:
      hgap = 1
      vgap = 1
      styleClass += "grid-table"
      styleClass ++= extraGridStyles

  def cell(node: Node, extraStyles: String*): StackPane =
    new StackPane:
      children = Seq(node)
      styleClass += "grid-table-cell"
      styleClass ++= extraStyles

  def addCell(
               grid: GridPane,
               col: Int,
               row: Int,
               node: Node,
               extraStyles: String*
             ): StackPane =
    val wrapped = cell(node, extraStyles*)
    grid.add(wrapped, col, row)
    wrapped

  def addSpanCell(
                   grid: GridPane,
                   col: Int,
                   row: Int,
                   colSpan: Int,
                   rowSpan: Int,
                   node: Node,
                   extraStyles: String*
                 ): StackPane =
    val wrapped = cell(node, extraStyles*)
    grid.add(wrapped, col, row, colSpan, rowSpan)
    wrapped
