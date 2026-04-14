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

import cats.effect.unsafe.implicits.global
import fdswarm.logging.LazyStructuredLogging
import fdswarm.StartupInfo
import fdswarm.fx.FdLogUi.isMac
import fdswarm.fx.discovery.ContestDiscovery
import fdswarm.fx.qso.ContestEntry
import fdswarm.fx.utils.UiStyles
import fdswarm.logging.Locus.Startup
import fdswarm.replication.{NodeStatusHandler, StatusBroadcastService}
import fdswarm.util.{DurationFormat, NodeIdentityManager, OtelMetrics}
import jakarta.inject.{Inject, Singleton}
import javafx.embed.swing.SwingFXUtils
import scalafx.application.Platform
import scalafx.scene.image.Image
import scalafx.scene.layout.{BorderPane, StackPane}
import scalafx.scene.paint.Color
import scalafx.scene.shape.SVGPath
import scalafx.scene.{Node, Scene, SnapshotParameters}
import scalafx.stage.Stage

import java.time.{Duration, Instant}

@Singleton
final class FdLogUi @Inject() (
  contestEntry: ContestEntry,
  menus: FdLogMenus,
  repl: NodeStatusHandler,
  statusBroadcastService: StatusBroadcastService,
  nodeIdentityManager: NodeIdentityManager,
  otelMetrics: OtelMetrics,
  qsoStore: fdswarm.store.QsoStore,
  apiServer: fdswarm.api.ApiServer,
  startupInfo: StartupInfo,
  contestDiscovery:ContestDiscovery
) extends LazyStructuredLogging(Startup):

  def start(): Unit =
    contestDiscovery.start()
    val stage = FdLogUi.primaryStage
    val qsoNode: Node = contestEntry.node
    val centerPane = new StackPane:
      children = List(qsoNode)
    val root = new BorderPane:
      top = menus.menuBar
      center = centerPane

    setAppIcon()
    stage.title = "FdSwarm"
    statusBroadcastService.start()
    apiServer.start().unsafeRunAndForget()

    if isMac then
      try
        System.setProperty(
          "apple.awt.application.name",
          "FdSwarm"
        )

        if java.awt.Desktop.isDesktopSupported then
          val desktop = java.awt.Desktop.getDesktop
          if desktop.isSupported(java.awt.Desktop.Action.APP_ABOUT) then
            desktop.setAboutHandler(_ =>
              Platform.runLater {
                menus.showAboutDialog()
              }
            )
            logger.debug("Successfully registered macOS About handler")
          else logger.debug("macOS About handler not supported by Desktop")

          if desktop.isSupported(java.awt.Desktop.Action.APP_QUIT_HANDLER) then
            desktop.setQuitHandler((_, response) =>
              Platform.runLater {
                Platform.exit()
                response.performQuit()
              }
            )
            logger.debug("Successfully registered macOS Quit handler")
        else logger.debug("Desktop API not supported on this platform")
      catch case e: Exception =>
        logger.warn(
          "Could not set macOS handlers"
        )

    stage.title = s"FdSwarm@${nodeIdentityManager.ourNodeIdentity.toString}"
    val scene = new Scene(
      root,
      1100,
      800
    )
    UiStyles.applyTo(scene)
    stage.scene = scene

    stage.show()

    contestEntry.bandModeMatrixPane.onConfigRequest = Some(
      () => menus.showBandModeManager()
    )

    Platform.runLater {
      val duration = FdLogUi.startupDuration
      val durationNanos = duration.toNanos
      logger.info(s"UI responsive in ${DurationFormat(duration)}")
      otelMetrics.recordTimerNanos(
        name = "fdswarm_startup_time_seconds",
        nanos = durationNanos
      )
    }
    contestEntry.buildUi()

  private def setAppIcon(): Unit =
    val stage = FdLogUi.primaryStage
    try
      val resource = getClass.getResource("/icons/fdswarm.svg")
      if resource != null then
        val svgContent = scala.io.Source.fromResource("icons/fdswarm.svg").mkString
        val pathRegex = "d=\"([^\"]+)\"".r
        val paths = pathRegex.findAllMatchIn(svgContent).map(_.group(1)).toList
        if paths.nonEmpty then
          val combinedPathValue = paths.mkString(" ")
          val svgPath = new SVGPath:
            content = combinedPathValue
            fill = Color.Transparent
            stroke = Color.Black
            strokeWidth = 2.5

          val params = new SnapshotParameters:
            fill = Color.Transparent

          val stackPane = new StackPane:
            children = Seq(svgPath)
          val iconImage = stackPane.snapshot(
            params,
            null
          )
          stage.getIcons.add(iconImage)

          try
            if java.awt.Taskbar.isTaskbarSupported then
              val taskbar = java.awt.Taskbar.getTaskbar
              if taskbar.isSupported(java.awt.Taskbar.Feature.ICON_IMAGE) then
                val bufferedImage = SwingFXUtils.fromFXImage(
                  iconImage,
                  null
                )
                taskbar.setIconImage(bufferedImage)
                logger.debug("Successfully set app icon via Taskbar")
          catch
            case e: Exception =>
              logger.debug(
                "Could not set app icon via Taskbar (this is normal on some platforms/JDKs)"
              )

      val pngResource = getClass.getResource("/icons/icon_256.png")
      if pngResource != null then
        val pngImage = new Image(pngResource.toExternalForm)
        stage.getIcons.add(pngImage)

        try
          if java.awt.Taskbar.isTaskbarSupported then
            val taskbar = java.awt.Taskbar.getTaskbar
            if taskbar.isSupported(java.awt.Taskbar.Feature.ICON_IMAGE) then
              val bufferedImage = SwingFXUtils.fromFXImage(
                pngImage,
                null
              )
              taskbar.setIconImage(bufferedImage)
        catch
          case _: Exception => ()
    catch case e: Exception =>
      logger.warn(
        "Could not set application icon"
      )

  def stopApp(): Unit =
    statusBroadcastService.stop()

object FdLogUi:
  private val startTime = Instant.now()
  private var stageOpt: Option[Stage] = None

  def startupDuration: Duration =
    Duration.between(
      startTime,
      Instant.now()
    )

  lazy val isMac: Boolean =
    System.getProperty("os.name").toLowerCase.contains("mac")

  def setPrimaryStage(
    stage: Stage
  ): Unit =
    stageOpt = Some(
      stage
    )

  def primaryStage: Stage =
    stageOpt.getOrElse(
      throw IllegalStateException(
        "Primary stage has not been initialized."
      )
    )
