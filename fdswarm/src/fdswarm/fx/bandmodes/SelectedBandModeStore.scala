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

  val selected: ObjectProperty[Option[BandMode]] =
    ObjectProperty(load())

  selected.onChange { (_, _, nv) =>
    persist(nv)
  }

  def current: Option[BandMode] =
    selected.value

  def set(value: Option[BandMode]): Unit =
    selected.value = value

  private def load(): Option[BandMode] =
    if !os.exists(path) then None
    else
      try read[Option[BandMode]](os.read(path))
      catch case _: Throwable => None

  private def persist(value: Option[BandMode]): Unit =
    val json = write(value, indent = 2)
    val tmp  = path / os.up / s".${path.last}.tmp"
    os.write.over(tmp, json, createFolders = true)
    os.move.over(tmp, path)