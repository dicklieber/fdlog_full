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
import fdswarm.FdLogApp
import fdswarm.FdLogApp.injector
import fdswarm.fx.FdLogUi.isMac
import fdswarm.fx.bandmodes.BandsAndModesPane
import fdswarm.fx.contest.ContestManager
import fdswarm.fx.qso.ContestEntry
import fdswarm.fx.station.StationEditor
import fdswarm.fx.tools.{ContestTimeDialog, FdHourDialogService, FdHourDigestsPane, HowManyDialogService, LoggingDialog, StatusBroadcastDialog}
import fdswarm.replication.{NodeStatusHandler, NodeStatusSender, SwarmStatusPane}
import fdswarm.store.FdHourDigest
import fdswarm.util.{DurationFormat, HostAndPortProvider}
import io.micrometer.core.instrument.MeterRegistry
import jakarta.inject.Inject
import java.util.concurrent.TimeUnit
import scalafx.Includes.*
import scalafx.beans.property.{BooleanProperty, StringProperty}
import scalafx.application.Platform
import scalafx.scene.{Node, Scene}
import scalafx.scene.control.*
import scalafx.scene.layout.*
import scalafx.stage.{Stage, Window}

import java.time.Duration

final class FdLogUi @Inject()(
                               contestEntry: ContestEntry,
                               bandModeManagerPane: BandsAndModesPane,
                               stationEditor: StationEditor,
                               contestManager: ContestManager,
                               howManyDialogService: HowManyDialogService,
                               fdHourDialogService: FdHourDialogService,
                               statusBroadcastDialog: StatusBroadcastDialog,
                               loggingDialog: LoggingDialog,
                               contestTimeDialog: ContestTimeDialog,
                               fdHourDigestsPane: FdHourDigestsPane,
                               repl: NodeStatusHandler,
                               swarmStatusPane: SwarmStatusPane,
                               nodeStatusService: NodeStatusSender,
                               aboutMenuItem: AboutMenuItem,
                               hostAndPortProvider: HostAndPortProvider,
                               userConfig: UserConfig,
                               userConfigEditor: UserConfigEditor,
                               meterRegistry: MeterRegistry,
                               webSessionsAdmin: fdswarm.fx.admin.WebSessionsAdmin,
                               qsoStore: fdswarm.store.QsoStore,
                               directoryProvider: fdswarm.io.DirectoryProvider,
                               filenameStamp: fdswarm.util.FilenameStamp,
                               exportDialog: fdswarm.fx.tools.ExportDialog
                             ) extends LazyLogging:

  private val qsoNode: Node =
    contestEntry.node
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

  private val devMenu: Menu =
    new Menu("Dev"):
      visible = false
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
        ,
        new MenuItem("Status Broadcast Settings"):
          onAction = _ =>
            Option(ownerWindow) match
              case Some(w) => statusBroadcastDialog.show(w)
              case None => ()
        ,
        new MenuItem("Logging"):
          onAction = _ =>
            Option(ownerWindow) match
              case Some(w) => loggingDialog.show(w)
              case None => ()
        ,
        new MenuItem("Contest Time"):
          onAction = _ =>
            Option(ownerWindow) match
              case Some(w) => contestTimeDialog.show(w)
              case None => ()
      )

  private val developerModeMenuItem = new CheckMenuItem("Developer Mode")
  developerModeMenuItem.selected <==> userConfig.getProperty[BooleanProperty]("developerMode")

  devMenu.visible <== developerModeMenuItem.selected

  private val userConfigMenuItem: MenuItem =
    new MenuItem("User Config"):
      onAction = _ =>
        Option(ownerWindow) match
          case Some(w) => userConfigEditor.show(w)
          case None => ()

  private val adminMenu: Menu =
    new Menu("Admin"):
      items = Seq(
        new MenuItem("Web Sessions"):
          onAction = _ =>
            Option(ownerWindow) match
              case Some(w) => webSessionsAdmin.show(w)
              case None => ()
      )

  private val menuBar = new MenuBar:
    useSystemMenuBar = isMac
    menus = Seq(
      fileMenu,
      configMenu,
      adminMenu,
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
            logger.debug("Successfully registered macOS About handler")
          else
            logger.debug("macOS About handler not supported by Desktop")
        else
          logger.debug("Desktop API not supported on this platform")
      catch
        case e: Exception => logger.warn("Could not set macOS about handler", e)

    stationMenuItem.disable = false
    contestMenuItem.disable = false

    stage.title = s"FdSwarm@${hostAndPortProvider.http}"
    stage.scene = new Scene(root, 1100, 800):
      stylesheets = Seq(getClass.getResource("/styles/app.css").toExternalForm)
    stage.show()

    Platform.runLater {
      val duration = FdLogApp.startupDuration
      val durationNanos = duration.toNanos
      logger.info(s"UI responsive in ${DurationFormat(duration)}")
      fdswarm.util.MetricsHelpers.recordTimerNanos(meterRegistry, "fdswarm_startup_time_seconds", durationNanos)
    }

  private def fileMenu: Menu =
    new Menu("File"):
      items = Seq(
        new MenuItem("Export"):
          onAction = _ =>
            Option(ownerWindow) match
              case Some(w) => exportDialog.show(w)
              case None => ()
        ,
        new MenuItem("Exit"):
          onAction = _ => Platform.exit()
      )

  private def configMenu: Menu =
    new Menu("Config"):
      items = Seq(
        new MenuItem("Band / Mode Manager"):
          onAction = _ =>
            Option(ownerWindow) match
              case Some(w) => bandModeManagerPane.show(w)
              case None => ()
        ,
        stationMenuItem,
        contestMenuItem,
        new SeparatorMenuItem(),
        userConfigMenuItem,
        developerModeMenuItem
      )

  private def helpMenu: Menu =
    new Menu("Help"):
      items = Seq(aboutMenuItem)

object FdLogUi:
  lazy val isMac: Boolean =
    System.getProperty("os.name").toLowerCase.contains("mac")
