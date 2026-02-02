package fdswarm.fx.bands

import fdswarm.io.DirectoryProvider
import jakarta.inject.{Inject, Named, Provider, Singleton}
import scalafx.collections.{ObservableBuffer, ObservableHashSet}
import scalafx.scene.control.ComboBox
import scalafx.scene.layout.Pane
import scalafx.Includes.*
import upickle.default.*
import fdswarm.fx.caseForm.ChoiceField
import fdswarm.model.BandMode.Band

/**
 * User can select which bands are available for QSOs.
 *
 * @param dirProvider where files go.
 */
@Singleton
final class AvailableBandsManager @Inject()(
                                             dirProvider: DirectoryProvider
                                           ):

  private val path: os.Path =
    dirProvider() / "bands.json"
  private var _current: Seq[Band] = loadFromDisk()

  def bands: Seq[Band] = _current

  def save(availableBands: Iterable[Band]): Unit =
    _current = availableBands.toSeq
    persistToDisk()

  private def persistToDisk(): Unit =
    val json = write(_current, indent = 2)
    os.write.over(path, json, createFolders = true)

  private def loadFromDisk(): Seq[Band] =
    try
      val json = os.read(path)
      read(json)
    catch
      case _: Throwable =>
        Seq("20m")
