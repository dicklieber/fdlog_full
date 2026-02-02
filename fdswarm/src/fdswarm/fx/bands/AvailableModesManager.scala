package fdswarm.fx.bands

import fdswarm.io.DirectoryProvider
import fdswarm.model.BandMode.Mode
import jakarta.inject.{Inject, Singleton}
import scalafx.collections.ObservableBuffer
import javafx.collections.ListChangeListener
import upickle.default.*

@Singleton
final class AvailableModesManager @Inject()(
                                             dirProvider: DirectoryProvider
                                           ):
  private val path: os.Path =
    dirProvider() / "modes.json"

  /** Single source of truth (ScalaFX-friendly) */
  val modes: ObservableBuffer[Mode] =
    ObservableBuffer.from(loadFromDisk())

  // Persist automatically when the buffer changes (same pattern as AvailableBandsManager)
  modes.delegate.addListener(
    new ListChangeListener[Mode]:
      override def onChanged(c: ListChangeListener.Change[? <: Mode]): Unit =
        persist()
  )

  /** Replace everything in the buffer from a Seq[Mode] */
  def setModes(newModes: Seq[Mode]): Unit =
    modes.setAll(newModes*)

  // ---- persistence ------------------------------------------------------------

  private def persist(): Unit =
    val json = write(modes.toSeq, indent = 2)
    os.write.over(path, json, createFolders = true)

  private def loadFromDisk(): Seq[Mode] =
    try
      read[Seq[Mode]](os.read(path))
    catch
      case _: Throwable =>
        Seq("SSB")