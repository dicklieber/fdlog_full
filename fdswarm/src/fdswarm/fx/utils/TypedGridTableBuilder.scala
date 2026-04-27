package fdswarm.fx.utils
import scalafx.Includes.*
import scalafx.beans.property.ObjectProperty
import scalafx.geometry.{Insets, Pos}
import scalafx.scene.Node
import scalafx.scene.control.Label
import scalafx.scene.layout.*

sealed trait GridCellValue

object GridCellValue:
  final case class Text(value: String) extends GridCellValue
  final case class NodeValue(node: Node) extends GridCellValue

enum GridColumnAlignment:
  case Left, Center, Right

object GridColumnAlignment:
  def styleClassOf(alignment: GridColumnAlignment): String =
    alignment match
      case GridColumnAlignment.Left   => "grid-align-left"
      case GridColumnAlignment.Center => "grid-align-center"
      case GridColumnAlignment.Right  => "grid-align-right"

sealed trait GridColumnWidth

object GridColumnWidth:
  final case class Fixed(value: Double) extends GridColumnWidth
  final case class Flexible(pref: Double) extends GridColumnWidth

  def fixed(value: Double): GridColumnWidth = Fixed(value)
  def flexible(pref: Double): GridColumnWidth = Flexible(pref)

final case class GridRowBehavior[A](
                                     rowStyleClasses: A => Seq[String] = (_: A) => Seq.empty[String],
                                     onClick: Option[A => Unit] = None,
                                     onDoubleClick: Option[A => Unit] = None
                                   )

object GridRowBehavior:
  def apply[A](
                rowStyleClasses: A => Seq[String],
                onClick: A => Unit
              ): GridRowBehavior[A] =
    new GridRowBehavior[A](
      rowStyleClasses = rowStyleClasses,
      onClick = Some(onClick),
      onDoubleClick = None
    )

  def empty[A]: GridRowBehavior[A] =
    GridRowBehavior[A]()

final case class GridColumn[A](
                                header: String,
                                render: A => GridCellValue,
                                cellStyleClasses: A => Seq[String] = (_: A) => Seq.empty[String],
                                sortValue: Option[A => String] = None,
                                headerStyleClasses: Seq[String] = Seq.empty,
                                alignment: GridColumnAlignment = GridColumnAlignment.Left,
                                width: GridColumnWidth = GridColumnWidth.flexible(Region.USE_COMPUTED_SIZE)
                              )

object GridColumn:

  def text[A](
               header: String,
               value: A => String,
               cellStyleClasses: A => Seq[String] = (_: A) => Seq.empty[String],
               sortable: Boolean = false,
               sortValue: Option[A => String] = None,
               headerStyleClasses: Seq[String] = Seq.empty,
               alignment: GridColumnAlignment = GridColumnAlignment.Left,
               width: GridColumnWidth = GridColumnWidth.flexible(Region.USE_COMPUTED_SIZE)
             ): GridColumn[A] =
    GridColumn(
      header = header,
      render = (a: A) => GridCellValue.Text(value(a)),
      cellStyleClasses = cellStyleClasses,
      sortValue =
        if sortable then Some(sortValue.getOrElse(value))
        else None,
      headerStyleClasses = headerStyleClasses,
      alignment = alignment,
      width = width
    )

  def node[A](
               header: String,
               value: A => Node,
               cellStyleClasses: A => Seq[String] = (_: A) => Seq.empty[String],
               sortValue: Option[A => String] = None,
               headerStyleClasses: Seq[String] = Seq.empty,
               alignment: GridColumnAlignment = GridColumnAlignment.Left,
               width: GridColumnWidth = GridColumnWidth.flexible(Region.USE_COMPUTED_SIZE)
             ): GridColumn[A] =
    GridColumn(
      header = header,
      render = (a: A) => GridCellValue.NodeValue(value(a)),
      cellStyleClasses = cellStyleClasses,
      sortValue = sortValue,
      headerStyleClasses = headerStyleClasses,
      alignment = alignment,
      width = width
    )

final class GridHeaderLabel(text0: String = "") extends Label(text0):
  styleClass += "grid-header-label"
  maxWidth = Double.MaxValue

object GridHeaderLabel:
  def apply(text: String): GridHeaderLabel = new GridHeaderLabel(text)

final class GridBodyLabel(text0: String = "") extends Label(text0):
  styleClass += "grid-body-label"
  maxWidth = Double.MaxValue

object GridBodyLabel:
  def apply(text: String): GridBodyLabel = new GridBodyLabel(text)

final class GridTableCell private (
                                    content0: Node,
                                    styleClassNames: Seq[String]
                                  ) extends StackPane:
  styleClass += "grid-table-cell"
  styleClass ++= styleClassNames
  alignment = Pos.CenterLeft
  padding = Insets(6, 8, 6, 8)
  maxWidth = Double.MaxValue
  children = Seq(content0)

object GridTableCell:
  def header(
              content: Node,
              extraStyleClasses: Seq[String] = Seq.empty
            ): GridTableCell =
    new GridTableCell(content, Seq("grid-header-cell") ++ extraStyleClasses)

  def body(
            content: Node,
            extraStyleClasses: Seq[String] = Seq.empty
          ): GridTableCell =
    new GridTableCell(content, Seq("grid-body-cell") ++ extraStyleClasses)

final case class GridSortState(
                                columnIndex: Int,
                                ascending: Boolean
                              )

final class TypedGridTableBuilder[A] private (
                                               val columns: Seq[GridColumn[A]],
                                               val rowBehavior: GridRowBehavior[A],
                                               val grid: GridPane
                                             ):

  private var configuredColumns = 0
  private var items: Vector[A] = Vector.empty

  val sortState: ObjectProperty[Option[GridSortState]] =
    ObjectProperty[Option[GridSortState]](this, "sortState", None)

  private def ensureColumnConstraints(requiredColumns: Int): Unit =
    if requiredColumns > configuredColumns then
      for i <- configuredColumns until requiredColumns do
        val c = columns(i)
        val cc = new ColumnConstraints
        c.width match
          case GridColumnWidth.Fixed(value) =>
            cc.hgrow = Priority.Never
            cc.fillWidth = true
            cc.minWidth = value
            cc.prefWidth = value
            cc.maxWidth = value
          case GridColumnWidth.Flexible(pref) =>
            cc.hgrow = Priority.Always
            cc.fillWidth = true
            cc.minWidth = Region.USE_COMPUTED_SIZE
            cc.prefWidth = pref
            cc.maxWidth = Double.MaxValue
        grid.columnConstraints.add(cc)
      configuredColumns = requiredColumns

  private def sortIndicatorFor(columnIndex: Int): String =
    sortState.value match
      case Some(GridSortState(idx, asc)) if idx == columnIndex =>
        if asc then " ▲" else " ▼"
      case _ => ""

  private def sortedItems: Vector[A] =
    sortState.value match
      case Some(GridSortState(columnIndex, ascending)) =>
        columns(columnIndex).sortValue match
          case Some(fn) =>
            val sorted = items.sortBy(fn)
            if ascending then sorted else sorted.reverse
          case None =>
            items
      case None =>
        items

  private def toggleSort(columnIndex: Int): Unit =
    columns(columnIndex).sortValue.foreach { _ =>
      val next =
        sortState.value match
          case Some(GridSortState(idx, asc)) if idx == columnIndex =>
            Some(GridSortState(columnIndex, !asc))
          case _ =>
            Some(GridSortState(columnIndex, true))
      sortState.value = next
      rebuild()
    }

  private def buildHeaderCell(col: GridColumn[A], idx: Int): GridTableCell =
    val label = GridHeaderLabel(col.header + sortIndicatorFor(idx))
    val cell = GridTableCell.header(
      label,
      col.headerStyleClasses ++
        Seq(GridColumnAlignment.styleClassOf(col.alignment)) ++
        (if col.sortValue.isDefined then Seq("grid-sortable-header-cell") else Seq.empty)
    )

    if col.sortValue.isDefined then
      cell.onMouseClicked = _ => toggleSort(idx)

    GridPane.setHgrow(cell, Priority.Always)
    cell

  private def buildBodyCell(
                             item: A,
                             rowIndex: Int,
                             col: GridColumn[A]
                           ): GridTableCell =
    val node: Node =
      col.render(item) match
        case GridCellValue.Text(v)      => GridBodyLabel(v)
        case GridCellValue.NodeValue(n) => n

    val zebra =
      if rowIndex % 2 == 0 then Seq("grid-row-even") else Seq("grid-row-odd")

    val cell = GridTableCell.body(
      node,
      zebra ++
        rowBehavior.rowStyleClasses(item) ++
        col.cellStyleClasses(item) ++
        Seq(GridColumnAlignment.styleClassOf(col.alignment))
    )

    rowBehavior.onClick.foreach { f =>
      cell.onMouseClicked = e =>
        if e.clickCount == 1 then f(item)
        rowBehavior.onDoubleClick.foreach { g =>
          if e.clickCount == 2 then g(item)
        }
    }

    if rowBehavior.onClick.isEmpty then
      rowBehavior.onDoubleClick.foreach { g =>
        cell.onMouseClicked = e =>
          if e.clickCount == 2 then g(item)
      }

    GridPane.setHgrow(cell, Priority.Always)
    cell

  def setItems(newItems: IterableOnce[A]): Unit =
    items = newItems.iterator.toVector
    rebuild()

  def rebuild(): Unit =
    grid.children.clear()
    ensureColumnConstraints(columns.length)

    columns.zipWithIndex.foreach { (c, i) =>
      grid.add(buildHeaderCell(c, i), i, 0)
    }

    sortedItems.zipWithIndex.foreach { (item, row) =>
      columns.zipWithIndex.foreach { (c, col) =>
        grid.add(buildBodyCell(item, row, c), col, row + 1)
      }
    }

object TypedGridTableBuilder:
  def apply[A](
                columns: GridColumn[A]*
              ): TypedGridTableBuilder[A] =
    apply[A](rowBehavior = GridRowBehavior.empty[A], columns = columns*)

  def apply[A](
                rowBehavior: GridRowBehavior[A],
                columns: GridColumn[A]*
              ): TypedGridTableBuilder[A] =
    val g = new GridPane:
      hgap = 0
      vgap = 0
      styleClass += "grid-table"

    new TypedGridTableBuilder[A](columns.toSeq, rowBehavior, g)