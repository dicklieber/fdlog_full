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

import com.typesafe.scalalogging.LazyLogging
import fdswarm.FdLogApp.injector
import fdswarm.fx.FdLogUi.isMac
import fdswarm.fx.bandmodes.BandsAndModesPane
import fdswarm.fx.contest.ContestManager
import fdswarm.fx.qso.ContestEntry
import fdswarm.fx.station.StationEditor
import fdswarm.fx.tools.{FdHourDialogService, FdHourDigestsPane, HowManyDialogService}
import fdswarm.replication.{NodeStatusHandler, NodeStatusSender, SwarmStatusPane}
import fdswarm.store.FdHourDigest
import fdswarm.util.HostAndPortProvider
import jakarta.inject.Inject
import scalafx.application.Platform
import scalafx.scene.{Node, Scene}
import scalafx.scene.control.*
import scalafx.scene.layout.*
import scalafx.stage.{Stage, Window}
import upickle.default.*

final class FdLogUi @Inject()(
                               contestEntry: ContestEntry,
                               bandModeManagerPane: BandsAndModesPane,
                               stationEditor: StationEditor,
                               contestManager: ContestManager,
                               howManyDialogService: HowManyDialogService,
                               fdHourDialogService: FdHourDialogService,
                               fdHourDigestsPane: FdHourDigestsPane,
                               repl: NodeStatusHandler,
                               swarmStatusPane: SwarmStatusPane,
                               nodeStatusService: NodeStatusSender,
                               aboutMenuItem: AboutMenuItem,
                               hostAndPortProvider: HostAndPortProvider
                             ) extends LazyLogging:

  private val bandModeNode: Node =
    bandModeManagerPane
  private val qsoNode: Node =
    contestEntry.node
  private val swarmStatusNode: Node =
    swarmStatusPane.node
  private val centerPane = new StackPane:
    children = List(qsoNode)
  private val contestMenuItem: MenuItem =
    new MenuItem("Contest"):
      disable = true
      onAction = _ =>
        Option(ownerWindow) match
          case Some(w) => contestManager.show(w)
          case None => ()

  private val stationMenuItem: MenuItem =
    new MenuItem("Station"):
      disable = true
      onAction = _ =>
        Option(ownerWindow) match
          case Some(w) => stationEditor.show(w)
          case None => ()

  private val menuBar = new MenuBar:
    useSystemMenuBar = isMac
    menus = Seq(
      fileMenu,
      viewMenu,
      configMenu,
      devMenu,
      helpMenu,
    )

  private val root = new BorderPane:
    top = menuBar
    center = centerPane
  private var ownerWindow: Window = null.asInstanceOf[Window]

  def start(stage: Stage): Unit =
    nodeStatusService.start()


    ownerWindow = stage
    aboutMenuItem.setOwner(stage)

    if isMac then
      try
        if java.awt.Desktop.isDesktopSupported then
          val desktop = java.awt.Desktop.getDesktop
          if desktop.isSupported(java.awt.Desktop.Action.APP_ABOUT) then
            desktop.setAboutHandler(_ =>
              Platform.runLater {
                aboutMenuItem.showAboutDialog(ownerWindow)
              }
            )
            logger.info("Successfully registered macOS About handler")
          else
            logger.info("macOS About handler not supported by Desktop")
        else
          logger.info("Desktop API not supported on this platform")
      catch
        case e: Exception => logger.warn("Could not set macOS about handler", e)

    stationMenuItem.disable = false
    contestMenuItem.disable = false

    stage.title = s"FdSwarm@${hostAndPortProvider.http}"
    stage.scene = new Scene(root, 1100, 800):
      stylesheets = Seq(getClass.getResource("/styles/app.css").toExternalForm)
    stage.show()

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
          onAction = _ => showPane(qsoNode),
        new MenuItem("Swarm Status"):
          onAction = _ => showPane(swarmStatusNode)
      )

  private def showPane(node: Node): Unit =
    centerPane.children.setAll(node)

  private def configMenu: Menu =
    new Menu("Config"):
      items = Seq(
        new MenuItem("Band / Mode Manager"):
          onAction = _ => showPane(bandModeNode)
        ,
        stationMenuItem,
        contestMenuItem
      )

  private def helpMenu: Menu =
    new Menu("Help"):
      items = Seq(aboutMenuItem)

  private def devMenu: Menu =
    new Menu("Dev"):
      items = Seq(
        new MenuItem("Generate QSOs"):
          onAction = _ =>
            Option(ownerWindow) match
              case Some(w) => howManyDialogService.showAndGenerate(w)
              case None => ()
        ,
        new MenuItem("Send FdHour"):
          onAction = _ =>
            Option(ownerWindow) match
              case Some(w) => fdHourDialogService.show(w)
              case None => ()
        ,
        new MenuItem("FdHours"):
          onAction = _ =>
            Option(ownerWindow) match
              case Some(w) => fdHourDigestsPane.show(w)
              case None => ()
        
/*
        new MenuItem("FdHour"):
          onAction = _ =>
            val base64 = repl.byFdHourJsonGzipBase64
            val decoded = java.util.Base64.getDecoder.decode(base64)
            val bais = new java.io.ByteArrayInputStream(decoded)
            val gzis = new java.util.zip.GZIPInputStream(bais)
            val json = new String(gzis.readAllBytes(), "UTF-8")
            println(s"Decoded JSON: $json")
            val s: Seq[FdHourDigest] = read(json)
            println(s"Decoded FdHourDigests: $s")
*/
        ,
/*
        new MenuItem("Broadcast FdHour"):
          onAction = _ =>
            val base64 = repl.byFdHourJsonGzipBase64
        //            broadcastSender.broadcast(base64)
*/
      )

object FdLogUi:
  lazy val isMac: Boolean =
    System.getProperty("os.name").toLowerCase.contains("mac")
