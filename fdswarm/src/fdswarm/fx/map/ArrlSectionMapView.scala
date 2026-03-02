package fdswarm.fx.map

import scalafx.Includes.*
import scalafx.application.Platform
import scalafx.beans.property.ObjectProperty
import scalafx.scene.layout.VBox
import scalafx.scene.web.WebView

import javafx.concurrent.Worker
import netscape.javascript.JSObject

trait SectionClickHandler:
  def onSectionClicked(id: String): Unit

/** WebView-based SVG map viewer.
 *
 * Expects an SVG where clickable regions are:
 *   <path id="..." class="section" ... />
 *
 * Your generated SVG already includes JS that calls window.app.sectionClicked(id),
 * but we also inject a fallback click-wiring JS (in case you later swap SVGs).
 */
final class ArrlSectionMapView(
                                svgText: String,
                                handler: SectionClickHandler = (id: String) => println(s"Clicked: $id")
                              ) extends VBox:

  private val webView = new WebView()
  children = webView

  /** Most recent clicked region id, e.g. "region_0042" */
  val lastClickedId: ObjectProperty[Option[String]] = ObjectProperty(None)

  /** Optional: your future mapping from region ids to real ARRL section codes */
  private var regionToSection: Map[String, String] = Map.empty

  def setRegionToSectionMap(m: Map[String, String]): Unit =
    regionToSection = m

  /** Convenience: set fill by region id, e.g. "region_0042" -> "#ff0000" */
  def setRegionColor(regionId: String, cssColor: String): Unit =
    runJs(s"""setFill(${jsString(regionId)}, ${jsString(cssColor)});""")

  /** Convenience: bulk colors */
  def setRegionColors(colors: Map[String, String]): Unit =
    colors.foreach { case (id, c) => setRegionColor(id, c) }

  /** If you have section codes mapped, set fill by section code */
  def setSectionColor(sectionCode: String, cssColor: String): Unit =
    // invert mapping on the fly (small map is fine)
    regionToSection.collectFirst { case (rid, sec) if sec == sectionCode => rid } match
      case Some(regionId) => setRegionColor(regionId, cssColor)
      case None => ()

  private class JsBridge:
    def sectionClicked(id: String): Unit =
      Platform.runLater {
        lastClickedId.value = Some(id)
        // If you mapped region->section, you can call handler with section code instead if you prefer:
        handler.onSectionClicked(regionToSection.getOrElse(id, id))
      }

  private val bridge = new JsBridge

  private val html = wrapSvgInHtml(svgText)
  webView.engine.loadContent(html)

  webView.engine.getLoadWorker.stateProperty.addListener { (_, _, s) =>
    if s == Worker.State.SUCCEEDED then
      val window = webView.engine.executeScript("window").asInstanceOf[JSObject]
      window.setMember("app", bridge) // JS can call window.app.sectionClicked("region_0042")
      // Ensure click wiring exists even if SVG doesn't include onclick handlers:
      runJs("wireSectionClicks();")
  }

  private def runJs(js: String): Unit =
    Platform.runLater {
      try webView.engine.executeScript(js)
      catch case _: Throwable => ()
    }

  private def wrapSvgInHtml(svg: String): String =
    s"""
<!doctype html>
<html>
<head>
<meta charset="utf-8"/>
<style>
  html, body { margin:0; padding:0; background:#ffffff; }
  #wrap { width:100%; height:100%; }
  svg { width:100%; height:100%; display:block; }
  .section { cursor: pointer; }
</style>
</head>
<body>
<div id="wrap">
$svg
</div>

<script>
  function byId(id) { return document.getElementById(id); }

  // Force-fill helper (works for your traced SVG paths)
  function setFill(id, cssColor) {
    const el = byId(id);
    if (el) el.style.fill = cssColor;
  }

  // Fallback: attach click handlers to anything with class="section"
  function wireSectionClicks() {
    const els = document.querySelectorAll(".section");
    els.forEach(el => {
      // If SVG already has onclick, this won't break anything.
      el.addEventListener("click", () => {
        const id = el.id || el.getAttribute("data-section") || "";
        if (window.app && typeof window.app.sectionClicked === "function") {
          window.app.sectionClicked(id);
        }
      });
    });
  }
</script>
</body>
</html>
"""

  private def jsString(s: String): String =
    "\"" + s
      .replace("\\", "\\\\")
      .replace("\"", "\\\"")
      .replace("\n", "\\n")
      .replace("\r", "") + "\""