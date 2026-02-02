package fdswarm.fx.bandmodes

import jakarta.inject.{Inject, Singleton}
import fdswarm.io.DirectoryProvider
import scalafx.beans.property.ObjectProperty
import upickle.default.*

@Singleton
final class SelectedBandModeStore @Inject() (dirProvider: DirectoryProvider):

  private val dir: os.Path =
    val p = dirProvider()
    os.makeDir.all(p)
    p

  private val path: os.Path =
    dir / "selected-bandmode.json"

  private var _selected: BandMode = BandMode("??", "??")


  def selected:BandMode = _selected

  def save(value: BandMode): Unit =
    _selected = value
    persist()

  private def load():Unit =
      try
        _selected = read[BandMode](os.read(path))
      catch case _: Throwable =>
        _selected = BandMode("20m", "Ph")

  private def persist(): Unit =
    val json = write(_selected, indent = 2)
    val tmp  = path / os.up / s".${path.last}.tmp"
    os.write.over(tmp, json, createFolders = true)
    os.move.over(tmp, path)