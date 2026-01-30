package fdswarm.fx.bands

import fdswarm.io.DirectoryProvider
import fdswarm.model.{AvailableBands, HamBand}
import jakarta.inject.*
import scalafx.scene.layout.Pane
import upickle.default.*

@Singleton
final class AvailableBandsStore @Inject()(
                                            dirProvider: DirectoryProvider,
                                            @Named("fdswarm.availableBands.dir") configuredDir: String,

                                            @Named("fdswarm.availableBands.fileName") fileName: String,

                                            hamBandPaneProvider: Provider[HamBandCheckBoxPane]
                                          ):

  private val baseDir: os.Path =
    if configuredDir != null && configuredDir.trim.nonEmpty then
      os.Path(configuredDir, os.pwd)
    else
      dirProvider()

  private val path: os.Path =
    os.makeDir.all(baseDir)
    baseDir / fileName

  private var current: AvailableBands =
    loadFromDisk().getOrElse(AvailableBands(Set.empty))

  /** Only UI entrypoint */
  def availableBandsPane: Pane =
    ui.pane

  /** In-memory value */
  def availableBands: AvailableBands =
    current

  /** Memory-only update */
  def setAvailableBands(v: AvailableBands): Unit =
    current = v

  def setSelectedBands(v: Set[HamBand]): Unit =
    setAvailableBands(AvailableBands(v))

  /** Save button calls this */
  def saveNow(): Unit =
    persistToDisk(current)

  // ---- private ----

  private lazy val ui: HamBandCheckBoxPane =
    hamBandPaneProvider.get()

  private def persistToDisk(v: AvailableBands): Unit =
    val names = v.bands.toSeq.map(_.bandName).distinct.sorted
    val json  = write(names, indent = 2)

    val tmp   = (path / os.up) / s".${path.last}.tmp"
    os.write.over(tmp, json, createFolders = true)
    os.move.over(tmp, path)

  private def loadFromDisk(): Option[AvailableBands] =
    if !os.exists(path) then None
    else
      try
        val json  = os.read(path)
        val names = read[Seq[String]](json).toSet
        val byName = HamBand.all.map(b => b.bandName -> b).toMap
        Some(AvailableBands(names.flatMap(byName.get)))
      catch
        case _: Throwable => None