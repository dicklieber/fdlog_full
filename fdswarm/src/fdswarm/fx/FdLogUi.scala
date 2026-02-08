package fdswarm.fx

import fdswarm.fx.bandmodes.BandsAndModesPane
import fdswarm.fx.contest.ContestManager
import fdswarm.fx.qso.ContestEntry
import fdswarm.fx.station.StationEditor
import fdswarm.fx.tools.HowManyDialogService
import jakarta.inject.Inject
import scalafx.application.Platform
import scalafx.event.EventIncludes.*
import scalafx.scene.Scene
import scalafx.scene.Node
import scalafx.scene.control.*
import scalafx.scene.layout.*
import scalafx.stage.{Stage, Window}

final class FdLogUi @Inject() (
                                contestEntry: ContestEntry,
                                bandModeManagerPane: BandsAndModesPane,
                                stationEditor: StationEditor,
                                contestManager: ContestManager,
                                howManyDialogService: HowManyDialogService
                              ):

  private val bandModeNode: Node =
    bandModeManagerPane

  private val qsoNode: Node =
    contestEntry.node

  private val centerPane = new StackPane:
    children = List(qsoNode)

  private var ownerWindow: Window = null.asInstanceOf[Window]

  private val contestMenuItem: MenuItem =
    new MenuItem("Contest"):
      disable = true
      onAction = _ =>
        Option(ownerWindow) match
          case Some(w) => contestManager.show(w)
          case None    => ()

  private val stationMenuItem: MenuItem =
    new MenuItem("Station"):
      disable = true
      onAction = _ =>
        Option(ownerWindow) match
          case Some(w) => stationEditor.show(w)
          case None    => ()

  private val menuBar = new MenuBar:
    menus = Seq(
      fileMenu,
      viewMenu,
      configMenu,
      devMenu
    )

  private val root = new BorderPane:
    top = menuBar
    center = centerPane

  def start(stage: Stage): Unit =
    ownerWindow = stage
    stationMenuItem.disable = false
    contestMenuItem.disable = false

    stage.title = "FDLog"
    stage.scene = new Scene(root, 1100, 800) {
      stylesheets = Seq(getClass.getResource("/styles/app.css").toExternalForm)
    }
    stage.show()

  private def showPane(node: Node): Unit =
    centerPane.children.setAll(node)

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
        new MenuItem("Band / Mode Manager") {
          onAction = _ => showPane(bandModeNode)
        },
        stationMenuItem,
        contestMenuItem
      )

  private def devMenu: Menu =
    new Menu("Dev"):
      items = Seq(
        new MenuItem("Generate QSOs"):
          onAction = _ =>
            Option(ownerWindow) match
              case Some(w) => howManyDialogService.showAndGenerate(w)
              case None    => ()
      )
