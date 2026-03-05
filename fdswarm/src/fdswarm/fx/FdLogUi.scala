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
import fdswarm.fx.tools.{ContestTimeDialog, FdHourDialogService, FdHourDigestsPane, HowManyDialogService, IpAddressDialogService, LoggingDialog, StatusBroadcastDialog}
import fdswarm.replication.{NodeStatusHandler, StatusBroadcastService, SwarmStatusPane}
import fdswarm.store.FdHourDigest
import fdswarm.util.{DurationFormat, HostAndPortProvider}
import io.micrometer.core.instrument.MeterRegistry
import jakarta.inject.Inject

import java.util.concurrent.TimeUnit
import scalafx.Includes.*
import scalafx.beans.binding.{Bindings, BooleanBinding}
import scalafx.beans.property.{BooleanProperty, StringProperty}
import scalafx.application.Platform
import scalafx.scene.{Node, Scene}
import scalafx.scene.control.*
import scalafx.scene.layout.*
import scalafx.scene.image.Image
import scalafx.stage.{Stage, Window}
import scalafx.scene.shape.SVGPath
import scalafx.scene.paint.Color
import scalafx.scene.SnapshotParameters
import scalafx.scene.Scene
import scalafx.Includes.*
import scalafx.scene.web.WebView
import javafx.concurrent.Worker
import netscape.javascript.JSObject

import scala.io.Source
import io.circe.generic.auto.*
import io.circe.parser.decode
import io.circe.syntax.*

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
                               statusBroadcastService: StatusBroadcastService,
                               aboutMenuItem: AboutMenuItem,
                               hostAndPortProvider: HostAndPortProvider,
                               userConfig: UserConfig,
                               userConfigEditor: UserConfigEditor,
                               meterRegistry: MeterRegistry,
                               qsoStore: fdswarm.store.QsoStore,
                               exportDialog: fdswarm.fx.tools.ExportDialog,
                               webSessionsAdmin: fdswarm.fx.admin.WebSessionsAdmin,
                               sectionsProvider: fdswarm.fx.sections.SectionsProvider,
                               sectionPanel: fdswarm.fx.sections.SectionPanel,
                               ipAddressDialogService: IpAddressDialogService
                             ) extends LazyLogging:

  // --- ARRL Sections Map (SVG) -------------------------------------------------

  private val labelArrlRegionsMenuItem = new CheckMenuItem("Label ARRL Regions"):
    selected = false

  // Persist the region->section mapping as JSON next to where you run the app.
  // You can move this to a user config directory later.
  private val arrlRegionMapPath: os.Path = os.pwd / "arrl-region-map.json"

  private var arrlRegionToSection: Map[String, String] = loadArrlRegionMap()

  private def loadArrlRegionMap(): Map[String, String] =
    if os.exists(arrlRegionMapPath) then
      val txt = os.read(arrlRegionMapPath)
      decode[Map[String, String]](txt) match
        case Right(m) => m
        case Left(err) =>
          logger.warn(s"Could not parse ${arrlRegionMapPath.toString}: ${err.getMessage}")
          Map.empty
    else Map.empty

  private def saveArrlRegionMap(m: Map[String, String]): Unit =
    try
      val json = m.asJson.spaces2
      os.write.over(arrlRegionMapPath, json)
    catch
      case e: Exception => logger.warn(s"Could not write ${arrlRegionMapPath.toString}", e)

  private val arrlSectionsMapMenuItem: MenuItem =
    new MenuItem("ARRL Sections Map"):
      onAction = _ =>
        Option(ownerWindow) match
          case Some(w) => showArrlSectionsMap(w)
          case None => ()

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
        ,
        new MenuItem("Set IP Address"):
          onAction = _ =>
            Option(ownerWindow) match
              case Some(w) => ipAddressDialogService.show(w)
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

  private def setAppIcon(stage: Stage): Unit = {
    try {
      // Use the SVG data from fdswarm.svg to create a Stage icon.
      // Since JavaFX Stage icons must be Images, we'll try to render it.
      val resource = getClass.getResource("/icons/fdswarm.svg")
      if (resource != null) {
        // Prefer loading as a direct Image if the runtime supports it (some modern JavaFX versions do for SVG)
        // But our manual snapshot approach is safer for older versions.
        val svgContent = os.read(os.Path(java.nio.file.Paths.get(resource.toURI)))
        val pathRegex = "d=\"([^\"]+)\"".r
        val paths = pathRegex.findAllMatchIn(svgContent).map(_.group(1)).toList
        if (paths.nonEmpty) {
          val combinedPathValue = paths.mkString(" ")
          val svgPath = new SVGPath {
            content = combinedPathValue
            fill = Color.Black
          }
          // Create a small icon image via snapshot
          val params = new SnapshotParameters {
            fill = Color.Transparent
          }
          val iconImage = svgPath.snapshot(params, null)
          stage.getIcons.add(iconImage)
        }
      }

      // Fallback: Add the PNG icon as well, JavaFX will pick the best resolution.
      val pngResource = getClass.getResource("/icons/icon_256.png")
      if (pngResource != null) {
        stage.getIcons.add(new Image(pngResource.toExternalForm))
      }
    } catch {
      case e: Exception => logger.warn("Could not set application icon", e)
    }
  }

  def start(stage: Stage): Unit =
    setAppIcon(stage)
    stage.title = "FdSwarm"
    statusBroadcastService.start()


    ownerWindow = stage
    aboutMenuItem.setOwner(stage)

    if isMac then
      try
        // Set the application name for AWT (often needed for macOS integration)
        System.setProperty("apple.awt.application.name", "FdSwarm")

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

          if desktop.isSupported(java.awt.Desktop.Action.APP_QUIT_HANDLER) then
            desktop.setQuitHandler((_, response) =>
              Platform.runLater {
                Platform.exit()
                response.performQuit()
              }
            )
            logger.debug("Successfully registered macOS Quit handler")
        else
          logger.debug("Desktop API not supported on this platform")
      catch
        case e: Exception => logger.warn("Could not set macOS handlers", e)

    stationMenuItem.disable = false
    contestMenuItem.disable = false

    stage.title = s"FdSwarm@${hostAndPortProvider.nodeIdentity}"
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
      val exportItem = new MenuItem("Export"):
        onAction = _ =>
          Option(ownerWindow) match
            case Some(w) => exportDialog.show(w)
            case None => ()
      val exitItem = new MenuItem("Exit"):
        onAction = _ => Platform.exit()

      items = if isMac then Seq(exportItem) else Seq(exportItem, exitItem)

  // NOTE: On macOS with `useSystemMenuBar = true`, `items = Seq(...)` can be finicky.
  // Using `items ++= ...` plus a stable `lazy val` is more reliable.
  private lazy val configMenu: Menu =
    new Menu("Config"):
      items ++= Seq(
        new MenuItem("Band / Mode Manager"):
          onAction = _ =>
            Option(ownerWindow) match
              case Some(w) => bandModeManagerPane.show(w)
              case None => ()
        ,
        stationMenuItem,
        contestMenuItem,
        new SeparatorMenuItem(),
        arrlSectionsMapMenuItem,
        labelArrlRegionsMenuItem,
        new SeparatorMenuItem(),
        userConfigMenuItem,
        developerModeMenuItem
      )

  private def showArrlSectionsMap(parentWindow: Window): Unit =
    val svgText =
      // Prefer bundling the SVG as a resource in your app jar:
      //   src/resources/maps/arrl_sections_autotrace.svg
      val res = getClass.getResourceAsStream("/maps/arrl_sections_autotrace.svg")
      if res != null then
        val s = Source.fromInputStream(res, "UTF-8")
        try s.mkString finally s.close()
      else
        // Dev fallback: if you’re running from the repo and haven’t moved it into resources yet.
        val dev = os.pwd / "arrl_sections_autotrace.svg"
        if os.exists(dev) then os.read(dev)
        else
          """<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"800\" height=\"200\">
            <rect width=\"100%\" height=\"100%\" fill=\"#f7f7f7\"/>
            <text x=\"20\" y=\"110\" font-size=\"20\" fill=\"#333\" font-family=\"system-ui\">
              Missing /maps/arrl_sections_autotrace.svg (bundle it in resources)
            </text>
          </svg>""".stripMargin

    val webView = new WebView()

    // Scala <-> JS bridge (hook for future code)
    final class JsBridge:
      private val allArrlSectionCodes: Seq[String] =
        sectionsProvider.allSections.map(_.code).distinct.sorted

      def getMappings: String =
        arrlRegionToSection.asJson.noSpaces

      def getAllSections: String =
        allArrlSectionCodes.asJson.noSpaces

      def isLabelingMode: Boolean =
        labelArrlRegionsMenuItem.selected.value

      private var activeRegionId: Option[String] = None
      def getActiveRegionId: Option[String] = activeRegionId

      def updateMapping(regionId: String, sectionCode: String): Unit =
        Platform.runLater {
          val section = sectionCode.trim.toUpperCase
          if section.nonEmpty then
            arrlRegionToSection = arrlRegionToSection.updated(regionId, section)
            saveArrlRegionMap(arrlRegionToSection)
            logger.info(s"ARRL region mapped via SectionPanel: $regionId -> $section")
            webView.engine.executeScript("refreshLabels();")
        }

      def sectionClicked(id: String): Unit =
        Platform.runLater {
          val mapped = arrlRegionToSection.get(id)
          logger.info(s"ARRL map clicked: $id${mapped.fold("")(s => s" -> $s")}")
          if labelArrlRegionsMenuItem.selected.value then
            activeRegionId = Some(id)
            // Visual feedback in JS that this region is selected for mapping
            webView.engine.executeScript(s"highlightRegion('$id');")
          webView.engine.executeScript("refreshLabels();")
        }

    val bridge = new JsBridge

    val mappingSectionField = new StringProperty("")
    val mappingCanSubmit = Bindings.createBooleanBinding(() => true) // Always allowed to map in this view

    mappingSectionField.onChange { (_, _, newValue) =>
      if newValue != null && newValue.nonEmpty then
        bridge.getActiveRegionId.foreach { regionId =>
          bridge.updateMapping(regionId, newValue)
        }
    }

    val sectionPanelNode = sectionPanel.buildNode(
      mappingSectionField,
      () => (), // No extra submit action needed
      mappingCanSubmit,
      "Select ARRL Section to Map"
    )

    def wrap(svg: String): String =
      s"""<!doctype html>
         |<html><head><meta charset=\"utf-8\"/>
         |<style>
         |  html, body { margin:0; padding:0; background:#ffffff; overflow: hidden; }
         |  svg { width:100vw; height:100vh; display:block; }
         |  .section-label {
         |    font-family: sans-serif;
         |    font-size: 14px;
         |    font-weight: bold;
         |    fill: black;
         |    pointer-events: none;
         |    text-anchor: middle;
         |    dominant-baseline: middle;
         |  }
         |  .section { cursor: pointer; }
         |  .section:hover { opacity: 0.8; }
         |  .active-region { stroke: red; stroke-width: 3px; }
         |</style>
         |</head><body>
         |$svg
         |<script>
         |  function highlightRegion(id) {
         |    document.querySelectorAll('.active-region').forEach(el => el.classList.remove('active-region'));
         |    const el = document.getElementById(id);
         |    if (el) el.classList.add('active-region');
         |  }
         |
         |  function refreshLabels() {
         |    if (!window.app) return;
         |    const mappings = JSON.parse(window.app.getMappings());
         |    const svg = document.querySelector('svg');
         |    
         |    // Remove existing labels
         |    document.querySelectorAll('.section-label').forEach(el => el.remove());
         |
         |    for (const [regionId, sectionCode] of Object.entries(mappings)) {
         |      const path = document.getElementById(regionId);
         |      if (path) {
         |        const bbox = path.getBBox();
         |        const text = document.createElementNS('http://www.w3.org/2000/svg', 'text');
         |        text.setAttribute('x', bbox.x + bbox.width / 2);
         |        text.setAttribute('y', bbox.y + bbox.height / 2);
         |        text.setAttribute('class', 'section-label');
         |        text.textContent = sectionCode;
         |        svg.appendChild(text);
         |        
         |        // Also update title for tooltip
         |        let title = path.querySelector('title');
         |        if (!title) {
         |          title = document.createElementNS('http://www.w3.org/2000/svg', 'title');
         |          path.appendChild(title);
         |        }
         |        title.textContent = sectionCode;
         |      }
         |    }
         |  }
         |
         |  // Fallback: attach click handlers to anything with class=\"section\".
         |  function wireSectionClicks() {
         |    const els = document.querySelectorAll('.section');
         |    els.forEach(el => {
         |      el.addEventListener('click', (e) => {
         |        const id = el.id || el.getAttribute('data-section') || '';
         |        if (window.app && typeof window.app.sectionClicked === 'function') {
         |          window.app.sectionClicked(id);
         |        }
         |      });
         |    });
         |  }
         |</script>
         |</body></html>""".stripMargin

    webView.engine.loadContent(wrap(svgText))
    webView.engine.getLoadWorker.stateProperty.addListener { (_, _, state) =>
      if state == Worker.State.SUCCEEDED then
        val window = webView.engine.executeScript("window").asInstanceOf[JSObject]
        window.setMember("app", bridge)
        // If the SVG already has onclick handlers, this is harmless.
        webView.engine.executeScript("wireSectionClicks();")
        webView.engine.executeScript("refreshLabels();")
    }

    val stage = new Stage:
      initOwner(parentWindow)
      title = "ARRL Sections Map"
      scene = new Scene(new VBox {
        children = Seq(
          webView,
          sectionPanelNode
        )
        VBox.setVgrow(webView, Priority.Always)
      }, 1000, 850)

    stage.show()

  private def helpMenu: Menu =
    new Menu("Help"):
      items = Seq(aboutMenuItem)

object FdLogUi:
  lazy val isMac: Boolean =
    System.getProperty("os.name").toLowerCase.contains("mac")
