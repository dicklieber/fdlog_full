package fdswarm.fx

import jakarta.inject.Inject

import scalafx.application.Platform
import scalafx.event.EventIncludes.*
import scalafx.scene.Scene
import scalafx.scene.Node
import scalafx.scene.control.*
import scalafx.scene.layout.*
import scalafx.stage.Stage

import fdswarm.fx.bandmodes.BandModeManagerPane

import scala.util.Try

final class FdLogUi @Inject() (
                                qsoEntryPanel: QsoEntryPanel,
                                bandModeManagerPane: BandModeManagerPane
                              ):

  // BandModeManagerPane extends BorderPane => it *is* a Node already
  private val bandModeNode: Node = bandModeManagerPane

  // QsoEntryPanel is a controller (not a Node). Extract its view Node.
  private val qsoNode: Node = extractNode(qsoEntryPanel)

  private val centerPane = new StackPane:
    children = List(qsoNode)

  private val menuBar = new MenuBar:
    menus = Seq(
      fileMenu,
      viewMenu,
      configMenu
    )

  private val root = new BorderPane:
    top = menuBar
    center = centerPane

  def start(stage: Stage): Unit =
    stage.title = "FDLog"
    stage.scene = new Scene(root, 1100, 800)
    stage.show()

  private def showPane(node: Node): Unit =
    centerPane.children.setAll(node)

  // ---------------- menus ----------------

  private def fileMenu: Menu =
    new Menu("File"):
      items = Seq(
        new MenuItem("Exit"):
          onAction = _ => Platform.exit()
      )

  private def viewMenu: Menu =
    new Menu("View"):
      items = Seq(
        new MenuItem("QSO Entry"):
          onAction = _ => showPane(qsoNode)
      )

  private def configMenu: Menu =
    new Menu("Config"):
      items = Seq(
        new MenuItem("Band / Mode Manager"):
          onAction = _ => showPane(bandModeNode)
      )

  // ---------------- node extraction ----------------
  // Tries common method/field names used for "view" nodes in controller-style panes.
  private def extractNode(any: AnyRef): Node =
    any match
      case n: Node => n
      case _ =>
        val methodNames =
          List(
            "root", "pane", "node", "view", "content",
            "ui", "mainPane", "mainNode", "layout", "container"
          )

        val fieldNames = methodNames

        def tryMethod(name: String): Option[Node] =
          Try(any.getClass.getMethod(name)).toOption
            .flatMap(m => Try(m.invoke(any)).toOption)
            .collect { case n: Node => n }

        def tryField(name: String): Option[Node] =
          Try(any.getClass.getDeclaredField(name)).toOption
            .flatMap { f =>
              Try {
                f.setAccessible(true)
                f.get(any)
              }.toOption
            }
            .collect { case n: Node => n }

        methodNames.iterator.flatMap(tryMethod).toSeq.headOption
          .orElse(fieldNames.iterator.flatMap(tryField).toSeq.headOption)
          .getOrElse {
            // If we get here, we don't know what your panel exposes.
            // Fail loud with a helpful message listing candidates.
            val available =
              any.getClass.getMethods.map(_.getName).distinct.sorted.mkString(", ")

            throw new IllegalStateException(
              s"QsoEntryPanel (${any.getClass.getName}) is not a scalafx.scene.Node, " +
                s"and no Node-returning member was found. " +
                s"Tried methods/fields: ${methodNames.mkString(", ")}. " +
                s"Available methods include: $available"
            )
          }