package fdswarm.fx.bandmodes

import jakarta.inject.{Inject, Singleton}
import fdswarm.io.DirectoryProvider
import scalafx.beans.property.ObjectProperty
import upickle.default.*

@Singleton
final class BandModeStore @Inject() (dirProvider: DirectoryProvider) {

  final case class BandModes(
                              bands:   Set[String],
                              modes:   Set[String],
                              enabled: Map[String, Set[String]]
                            )
  object BandModes:
    given ReadWriter[BandModes] = macroRW

  private val dir: os.Path = {
    val p = dirProvider()
    os.makeDir.all(p)
    p
  }

  private val path: os.Path = dir / "bandmodes.json"

  private def load(): BandModes =
    if os.exists(path) then
      read[BandModes](os.read(path))
    else
      BandModes(Set.empty, Set.empty, Map.empty)

  private def save(bm: BandModes): Unit = {
    val json = write(bm, indent = 2)
    val tmp  = path / os.up / s".${path.last}.tmp"
    os.write.over(tmp, json, createFolders = true)
    os.move.over(tmp, path)
  }

  private val state: ObjectProperty[BandModes] =
    ObjectProperty(load())

  state.onChange { (_, _, nv) =>
    save(nv)
  }

  def currentBandMode: BandModes =
    state.value

  def setBands(bands: Set[String]): Unit =
    state.value = state.value.copy(bands = bands)

  def setModes(modes: Set[String]): Unit =
    state.value = state.value.copy(modes = modes)

  def setEnabled(enabled: Map[String, Set[String]]): Unit =
    state.value = state.value.copy(enabled = enabled)

  /** Update only enabled matrix; keep selected bands/modes. */
  def updateEnabledOnly(enabled: Map[String, Set[String]]): Unit =
    state.value = state.value.copy(enabled = enabled)

  // ---- helpers for UI wiring ----

  def isEnabled(mode: String, band: String): Boolean =
    state.value.enabled.getOrElse(mode, Set.empty).contains(band)

  def bandsForMode(mode: String): Set[String] =
    state.value.enabled.getOrElse(mode, Set.empty)

  def modesForBand(band: String): Set[String] =
    state.value.enabled.collect { case (m, bs) if bs.contains(band) => m }.toSet
}
