package fdswarm.fx.bands

import fdswarm.io.DirectoryProvider
import jakarta.inject.{Inject, Named, Provider, Singleton}
import scalafx.collections.ObservableBuffer
import scalafx.scene.control.ComboBox
import scalafx.scene.layout.Pane
import scalafx.util.StringConverter
import upickle.default.*
import fdswarm.fx.caseForm.ChoiceField

@Singleton
final class AvailableBandsStore @Inject() (
                                            dirProvider: DirectoryProvider,

                                            @Named("fdswarm.availableBands.dir")
                                            configuredDir: String,

                                            @Named("fdswarm.availableBands.fileName")
                                            fileName: String,

                                            bandCatalog: HamBandCatalog,
                                            paneProvider: Provider[HamBandCheckBoxPane]
                                          ):

  private val baseDir: os.Path =
    if configuredDir != null && configuredDir.trim.nonEmpty then
      os.Path(configuredDir, os.pwd)
    else
      dirProvider()

  private val path: os.Path =
    os.makeDir.all(baseDir)
    baseDir / fileName

  // ---- in-memory state ----

  private var current: AvailableBands =
    loadFromDisk().getOrElse(AvailableBands(Set.empty))

  /** Only UI entry point */
  def availableBandsPane: Pane =
    ui.pane

  /** In-memory value */
  def availableBands: AvailableBands =
    current

  /** Memory-only update */
  def setBandNames(names: Set[String]): Unit =
    current = AvailableBands(names)

  /** Save button calls this */
  def saveNow(): Unit =
    persistToDisk(current)

  /** A ComboBox over *all configured* ham bands, with one selected.
   *
   * @param selectedBandName optional bandName to select initially; if not found, selects the first band (if any).
   */
  def hamBandComboBox(selectedBandName: Option[String] = None): ComboBox[HamBand] =
    // Prefer only enabled/checked bands; if none are enabled, fall back to all configured bands
    val enabledNames: Set[String] = availableBands.bandNames
    val enabledBands: List[HamBand] =
      if enabledNames.nonEmpty then
        bandCatalog.all.filter(b => enabledNames.contains(b.bandName))
      else
        bandCatalog.all

    val items = ObservableBuffer.from(enabledBands)

    val cb = new ComboBox[HamBand](items)

    // Display "40m", "20m", etc. instead of HamBand(...)
    cb.converter = new StringConverter[HamBand]:
      override def toString(b: HamBand): String =
        if b == null then "" else b.bandName

      override def fromString(s: String): HamBand =
        // ComboBox is not editable by default; this is just for completeness.
        bandCatalog.get(s).orNull

    // Initial selection
    val initial: Option[HamBand] =
      selectedBandName
        .flatMap(bandCatalog.get)
        .filter(b => items.contains(b))
        .orElse(items.headOption)

    initial.foreach(cb.selectionModel().select)

    cb

  /**
   * Wrap HamBand selection as a ChoiceField so MyCaseForm can render it.
   *
   * Choices are always *all configured* bands from HamBandCatalog (via hamBandComboBox).
   * The initial selection is either the provided selection (if any) or `current`.
   */
  def hamBandChoice(current: HamBand): ChoiceField[HamBand] =
    ChoiceField(
      value = current,
      build = (sel: Option[HamBand]) =>
        val selectedName = sel.map(_.bandName).orElse(Option(current).map(_.bandName))
        hamBandComboBox(selectedName)
    )
  // ---- private ----

  private lazy val ui: HamBandCheckBoxPane =
    paneProvider.get()

  private def persistToDisk(v: AvailableBands): Unit =
    val names = v.bandNames.toSeq.distinct.sorted
    val json  = write(names, indent = 2)

    val tmp = (path / os.up) / s".${path.last}.tmp"
    os.write.over(tmp, json, createFolders = true)
    os.move.over(tmp, path)

  private def loadFromDisk(): Option[AvailableBands] =
    if !os.exists(path) then None
    else
      try
        val json  = os.read(path)
        val names = read[Seq[String]](json).toSet
        // keep only bands still defined in config
        val filtered = names.filter(bandCatalog.get(_).nonEmpty)
        Some(AvailableBands(filtered))
      catch
        case _: Throwable => None


final case class AvailableBands(bandNames: Set[String])