package fdswarm.fx.bandmodes

import jakarta.inject.{Inject, Singleton}
import fdswarm.model.BandMode
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

  val selected: ObjectProperty[BandMode] = ObjectProperty(load())

  selected.onChange { (_, _, newValue) =>
    persist(newValue)
  }

  def save(value: BandMode): Unit =
    selected.value = value

  private def load(): BandMode =
      try {
        val bandMode = read[BandMode](os.read(path))
        bandMode
      }
      catch case _: Throwable =>
        BandMode("20m", "PH")

  private def persist(value: BandMode): Unit =
    val json = write(value, indent = 2)
    val tmp  = path / os.up / s".${path.last}.tmp"
    os.write.over(tmp, json, createFolders = true)
    os.move.over(tmp, path)