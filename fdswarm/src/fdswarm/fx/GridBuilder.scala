/*
 * Copyright (c) 2026. Dick Lieber, WA9NNN
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package fdswarm.fx

import fdswarm.util.camelToWords
import scalafx.Includes.*
import scalafx.geometry.{Insets, Pos}
import scalafx.scene.Node
import scalafx.scene.control.Label
import scalafx.scene.layout.{ColumnConstraints, GridPane, Region}

import java.text.NumberFormat
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger
/**
 * A builder for creating a GridPane with label-value pairs.
 *
 * @param header top line of the grid
 */
class GridBuilder(header: Option[String] = None):
  private val headerLabel = new Label():
    styleClass += "grid-header"
    maxWidth = Double.MaxValue
    maxHeight = Double.MaxValue
    minWidth = Region.USE_PREF_SIZE
    alignment = Pos.Center
    // will set col span later

  private val _grid = new GridPane()
  _grid.hgap = 10
  _grid.vgap = 2
  _grid.padding = Insets(5)
  def gridLinesVisible: Boolean = _grid.gridLinesVisible.value

  def hgap: Double = _grid.hgap.value
  def hgap_=(v: Double): Unit = _grid.hgap = v

  def vgap: Double = _grid.vgap.value
  def vgap_=(v: Double): Unit = _grid.vgap = v

  def style: String = _grid.style.value
  def style_=(v: String): Unit = _grid.style = v

  def padding: Insets = _grid.padding.value
  def padding_=(v: Insets): Unit = _grid.padding = v

  def columnConstraints: Seq[ColumnConstraints] = _grid.columnConstraints.toSeq.map(new ColumnConstraints(_))
  def columnConstraints_=(v: Seq[ColumnConstraints]): Unit =
    _grid.columnConstraints.setAll(v.map(_.delegate)*)

  def add(child: Node, columnIndex: Int, rowIndex: Int): Unit = _grid.add(child, columnIndex, rowIndex)
  def add(child: Node, columnIndex: Int, rowIndex: Int, colspan: Int, rowspan: Int): Unit = _grid.add(child, columnIndex, rowIndex, colspan, rowspan)
  private var maxValues = 0
  private val rowIdx = AtomicInteger()
  private var columnClasses: Map[Int, String] = Map.empty

  def this(header: String) =
    this(Option(header).filter(_.nonEmpty))

  /**
   * Assign a CSS class to a value column.
   *
   * @param columnIndex value-column index starting at 1. Column 0 (row labels) is ignored.
   * @param styleClassName class name to add to cells in that column
   */
  def setColumnClass(columnIndex: Int, styleClassName: String): GridBuilder =
    if columnIndex >= 1 && styleClassName.nonEmpty then
      columnClasses = columnClasses.updated(columnIndex, styleClassName)
      _grid.children.foreach: n =>
        val colIndex = GridPane.getColumnIndex(n)
        if colIndex == columnIndex then
          val delegate = n.delegate
          delegate match
            case l: javafx.scene.control.Label =>
              if !l.getStyleClass.contains(styleClassName) then
                l.getStyleClass.add(styleClassName)
            case r: javafx.scene.layout.Region =>
              if !r.getStyleClass.contains(styleClassName) then
                r.getStyleClass.add(styleClassName)
            case _ =>
    this

  header.foreach(h =>
    // Start with grid header, if any.
    headerLabel.text = h
    val row = rowIdx.getAndIncrement()
    _grid.add(headerLabel, 0, row)
  )

  def apply(label: String, value: Any*): GridBuilder =
    maxValues = maxValues.max(value.length)
    val row = rowIdx.getAndIncrement()
    if label.nonEmpty then
      val rowLabel = new Label(camelToWords(label)):
        styleClass += "grid-row-label"
        maxWidth = Double.MaxValue
        maxHeight = Double.MaxValue
        minWidth = Region.USE_PREF_SIZE
      _grid.add(rowLabel, 0, row)
    else
      val rowLabelFiller = new Label(""):
        styleClass += "grid-row-label"
        maxWidth = Double.MaxValue
        maxHeight = Double.MaxValue
        minWidth = Region.USE_PREF_SIZE
      _grid.add(rowLabelFiller, 0, row)

    value.zipWithIndex.foreach: (v, idx) =>
      val valueValue = GridBuilder.valueToLabel(v)
      val delegate = valueValue.delegate
      delegate match
        case l: javafx.scene.control.Label =>
          l.setMaxWidth(Double.MaxValue)
          l.setMaxHeight(Double.MaxValue)
          l.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE)
          if !l.getStyleClass.contains("grid-value") && !l.getStyleClass.contains("grid-row-label") && !l.getStyleClass.contains("grid-header") then
            l.getStyleClass.add("grid-value")
          columnClasses.get(idx + 1).foreach(className =>
            if !l.getStyleClass.contains(className) then
              l.getStyleClass.add(className)
          )
        case r: javafx.scene.layout.Region =>
          r.setMaxWidth(Double.MaxValue)
          r.setMaxHeight(Double.MaxValue)
          r.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE)
          if !r.getStyleClass.contains("grid-value") && !r.getStyleClass.contains("grid-row-label") && !r.getStyleClass.contains("grid-header") && !r.getStyleClass.contains("local-node-column") then
            r.getStyleClass.add("grid-value")
          columnClasses.get(idx + 1).foreach(className =>
            if !r.getStyleClass.contains(className) then
              r.getStyleClass.add(className)
          )
        case _ =>
      _grid.add(valueValue, idx + 1, row)

    this

  def result: GridPane =
    if header.isDefined then
      GridPane.setColumnSpan(headerLabel, maxValues + 1)

    // Fill in any missing cells in rows to ensure they have a background.
    val rowCount = rowIdx.get()
    for r <- 0 until rowCount do
      for c <- 0 to maxValues do
        val occupied = _grid.children.exists: n =>
          val rowIndex = GridPane.getRowIndex(n)
          val colIndex = GridPane.getColumnIndex(n)
          val colSpan = Option(GridPane.getColumnSpan(n)).map(_.toInt).getOrElse(1)
          val rowSpan = Option(GridPane.getRowSpan(n)).map(_.toInt).getOrElse(1)

          r >= rowIndex && r < rowIndex + rowSpan && c >= colIndex && c < colIndex + colSpan

        if !occupied then
          val filler = new Label(""):
            styleClass += "grid-value"
            columnClasses.get(c).foreach(styleClass += _)
            maxWidth = Double.MaxValue
            maxHeight = Double.MaxValue
            minWidth = Region.USE_PREF_SIZE
          _grid.add(filler, c, r)

    _grid

object GridBuilder:
  private val numberFormat = NumberFormat.getIntegerInstance(Locale.US)

  def valueToLabel(value: Any, styleClassStr: String = "grid-value"): Node =
    value match
      case node: scalafx.scene.Node =>
        node // already a node, just use it.
      case s: String =>
        new Label(s):
          styleClass += styleClassStr
          maxWidth = Double.MaxValue
          maxHeight = Double.MaxValue
          minWidth = Region.USE_PREF_SIZE
      case prop: scalafx.beans.property.StringProperty =>
        new Label:
          text <== prop
          styleClass += styleClassStr
          maxWidth = Double.MaxValue
          maxHeight = Double.MaxValue
          minWidth = Region.USE_PREF_SIZE
      case i: Int =>
        new Label(numberFormat.format(i)):
          styleClass += "gridNumber"
          styleClass += styleClassStr
          maxWidth = Double.MaxValue
          maxHeight = Double.MaxValue
          minWidth = Region.USE_PREF_SIZE
      case x =>
        new Label(x.toString):
          styleClass += styleClassStr
          maxWidth = Double.MaxValue
          maxHeight = Double.MaxValue
          minWidth = Region.USE_PREF_SIZE
