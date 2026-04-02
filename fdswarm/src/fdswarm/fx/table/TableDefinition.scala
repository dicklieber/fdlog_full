package fdswarm.fx.table

import javafx.beans.property.SimpleObjectProperty
import javafx.css.PseudoClass
import scalafx.Includes.*
import scalafx.collections.ObservableBuffer
import scalafx.geometry.Pos
import scalafx.scene.Node
import scalafx.scene.control.*
import scalafx.scene.layout.HBox

/** Description of style that can be applied to rows or cells. */
final case class CellStyle(
    styleClasses: Seq[String] = Seq.empty,
    inlineStyle: Option[String] = None,
    pseudoClasses: Seq[String] = Seq.empty
)

object CellStyle:
  val Empty: CellStyle = CellStyle()

/** How a cell should render itself. */
sealed trait CellValue:
  def sortText: String

object CellValue:

  final case class Text(value: String) extends CellValue:
    override def sortText: String = value

  final case class IntValue(value: Int) extends CellValue:
    override def sortText: String = value.toString

  final case class LongValue(value: Long) extends CellValue:
    override def sortText: String = value.toString

  final case class DoubleValue(value: Double, formatted: String = "") extends CellValue:
    override def sortText: String = if formatted.nonEmpty then formatted else value.toString

  final case class BooleanValue(
      value: Boolean,
      trueText: String = "Yes",
      falseText: String = "No"
  ) extends CellValue:
    override def sortText: String = if value then trueText else falseText

  final case class NodeValue(node: Node, textForSort: String = "") extends CellValue:
    override def sortText: String = textForSort

  final case class NodesValue(nodes: Seq[Node], textForSort: String = "") extends CellValue:
    override def sortText: String = textForSort

/** Alignment hint for a generated TableColumn. */
enum ColumnAlignment:
  case Left, Center, Right

/**
 * Typed column definition.
 *
 * A = row type
 * B = extracted value type used for styling/rendering decisions
 */
final case class ColumnDef[A, B](
    header: String,
    extract: A => B,
    render: B => CellValue,
    prefWidth: Option[Double] = None,
    minWidth: Option[Double] = None,
    maxWidth: Option[Double] = None,
    resizable: Boolean = true,
    sortable: Boolean = true,
    styleClasses: Seq[String] = Seq.empty,
    alignment: ColumnAlignment = ColumnAlignment.Left,
    cellStyle: (A, B) => CellStyle = ((_: A, _: B) => CellStyle.Empty),
    sortKey: Option[B => String] = None
)

/**
 * Implement this in the companion object of a case class to describe its table.
 */
trait TableDefinition[A]:

  def title(count: Int): String

  def rowStyle(row: A): CellStyle = CellStyle.Empty

  def columns: Seq[ColumnDef[A, ?]]

  /**
   * Build a TableView from the supplied rows.
   */
  final def buildTable(rows: Seq[A]): TableView[A] =
    val table = new TableView[A](ObservableBuffer.from(rows))
    buildColumns().foreach(col => table.columns += col)
    table.rowFactory = { (_: TableView[A]) => buildRow() }
    table

  /** Convenience label you can place above the table. */
  final def buildTitleLabel(rows: Seq[A]): Label =
    new Label(title(rows.size))

  private def buildColumns(): Seq[TableColumn[A, ?]] =
    columns.map(buildTypedColumn(_))

  private def buildTypedColumn[B](columnDef: ColumnDef[A, B]): TableColumn[A, B] =
    val col = new TableColumn[A, B](columnDef.header)

    columnDef.prefWidth.foreach(col.prefWidth = _)
    columnDef.minWidth.foreach(col.minWidth = _)
    columnDef.maxWidth.foreach(col.maxWidth = _)
    col.resizable = columnDef.resizable
    col.sortable = columnDef.sortable
    if columnDef.styleClasses.nonEmpty then
      col.styleClass ++= columnDef.styleClasses

    col.cellValueFactory = { features =>
      new SimpleObjectProperty[B](columnDef.extract(features.value))
    }

    col.comparator = (left: B, right: B) =>
      val leftKey = columnDef.sortKey.map(_(left)).getOrElse(columnDef.render(left).sortText)
      val rightKey = columnDef.sortKey.map(_(right)).getOrElse(columnDef.render(right).sortText)
      leftKey.compareToIgnoreCase(rightKey)

    col.cellFactory = { (_: TableColumn[A, B]) =>
      new TableCell[A, B]:
        private def refresh(item: B): Unit =
          clearCellPresentation(this)
          if !empty.value && item != null then
            val rowItem = Option(tableRow.value).flatMap(r => Option(r.item.value))
            rowItem.foreach { row =>
              val rendered = columnDef.render(item)
              renderCellValue(this, rendered)
              applyAlignment(this, columnDef.alignment)
              applyCellStyle(this, columnDef.cellStyle(row, item))
            }
        item.onChange { (_, _, newItem) =>
          refresh(newItem)
        }
        empty.onChange { (_, _, _) =>
          refresh(item.value)
        }
    }
    col

  private def buildRow(): TableRow[A] =
    val row = new TableRow[A]
    row.item.onChange { (_, _, item) =>
      clearRowPresentation(row)
      if item != null && !row.empty.value then
        applyRowStyle(row, rowStyle(item))
    }
    row.empty.onChange { (_, _, _) =>
      if row.empty.value || row.item.value == null then
        clearRowPresentation(row)
      else
        clearRowPresentation(row)
        applyRowStyle(row, rowStyle(row.item.value))
    }
    row

  private def renderCellValue(cell: TableCell[A, ?], cellValue: CellValue): Unit =
    cellValue match
      case CellValue.Text(value) =>
        cell.text = value
      case CellValue.IntValue(value) =>
        cell.text = value.toString
      case CellValue.LongValue(value) =>
        cell.text = value.toString
      case CellValue.DoubleValue(value, formatted) =>
        cell.text = if formatted.nonEmpty then formatted else value.toString
      case CellValue.BooleanValue(value, trueText, falseText) =>
        cell.text = if value then trueText else falseText
      case CellValue.NodeValue(node, _) =>
        cell.graphic = node
        cell.contentDisplay = ContentDisplay.GraphicOnly
      case CellValue.NodesValue(nodes, _) =>
        cell.graphic = new HBox:
          spacing = 6
          alignment = Pos.CenterLeft
          children = ObservableBuffer.from(nodes)
        cell.contentDisplay = ContentDisplay.GraphicOnly

  private def applyAlignment(cell: TableCell[A, ?], alignment: ColumnAlignment): Unit =
    alignment match
      case ColumnAlignment.Left   => cell.alignment = Pos.CenterLeft
      case ColumnAlignment.Center => cell.alignment = Pos.Center
      case ColumnAlignment.Right  => cell.alignment = Pos.CenterRight

  private def applyCellStyle(cell: TableCell[A, ?], styleDef: CellStyle): Unit =
    if styleDef.styleClasses.nonEmpty then
      cell.styleClass ++= styleDef.styleClasses
    styleDef.inlineStyle.foreach(cell.style = _)
    setPseudoClasses(cell, styleDef.pseudoClasses)

  private def applyRowStyle(row: TableRow[A], styleDef: CellStyle): Unit =
    if styleDef.styleClasses.nonEmpty then
      row.styleClass ++= styleDef.styleClasses
    styleDef.inlineStyle.foreach(row.style = _)
    setPseudoClasses(row, styleDef.pseudoClasses)

  private def setPseudoClasses(node: Node, names: Seq[String]): Unit =
    names.foreach { name =>
      node.pseudoClassStateChanged(PseudoClass.getPseudoClass(name), true)
    }

extension [A](tableDefinition: TableDefinition[A])
  def tableView(rows: Seq[A]): TableView[A] =
    tableDefinition.buildTable(rows)

private def clearCellPresentation[A](cell: TableCell[A, ?]): Unit =
  cell.text = null
  cell.graphic = null
  cell.style = ""
  cell.contentDisplay = ContentDisplay.TextOnly
  val removable = cell.styleClass.filterNot(isBaseCellStyleClass).toSeq
  if removable.nonEmpty then cell.styleClass.removeAll(removable*)
  clearPseudoClasses(cell)

private def clearRowPresentation[A](row: TableRow[A]): Unit =
  row.style = ""
  val removable = row.styleClass.filterNot(isBaseRowStyleClass).toSeq
  if removable.nonEmpty then row.styleClass.removeAll(removable*)
  clearPseudoClasses(row)

private def clearPseudoClasses(node: Node): Unit =
  val active = node.getPseudoClassStates.toArray.map(_.toString)
  active.foreach { name =>
    node.pseudoClassStateChanged(PseudoClass.getPseudoClass(name), false)
  }

private def isBaseCellStyleClass(name: String): Boolean =
  name == "cell" ||
  name == "indexed-cell" ||
  name == "table-cell"

private def isBaseRowStyleClass(name: String): Boolean =
  name == "cell" ||
  name == "indexed-cell" ||
  name == "table-row-cell"
