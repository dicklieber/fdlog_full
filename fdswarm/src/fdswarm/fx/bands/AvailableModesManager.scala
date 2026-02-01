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
final class AvailableModesManager @Inject()(
                                             dirProvider: DirectoryProvider
                                           ):
  private val path: os.Path =
    dirProvider() / "modes.json"
  private var _current: Set[Band] = loadFromDisk()

  def save(availableBands: Iterable[Band]): Unit =
    _current = availableBands.toSet
    persistToDisk()

  def modes: Seq[Band] = _current.toSeq.sorted

  private def persistToDisk(): Unit =
    val json = write(_current, indent = 2)
    os.write.over(path, json, createFolders = true)

  private def loadFromDisk(): Set[Band] =
    try
      val json = os.read(path)
      read(json)
    catch
      case _: Throwable =>
        Set("20m")

