package fdswarm.fx.bands

import fdswarm.io.DirectoryProvider
import jakarta.inject.{Inject, Singleton}
import upickle.default.*
import fdswarm.model.BandMode.Band

import scalafx.collections.ObservableBuffer
import javafx.collections.ListChangeListener

@Singleton
final class AvailableBandsManager @Inject()(
                                             dirProvider: DirectoryProvider
                                           ):
  private val path: os.Path =
    dirProvider() / "bands.json"

  /** Single source of truth */
  val bands: ObservableBuffer[Band] =
    ObservableBuffer.from(loadFromDisk())

  // Persist automatically when the buffer changes
  bands.delegate.addListener(
    new ListChangeListener[Band]:
      override def onChanged(c: ListChangeListener.Change[? <: Band]): Unit =
        persist()
  )

  // ---- persistence ------------------------------------------------------------

  private def persist(): Unit =
    val json = write(bands.toSeq, indent = 2)
    os.write.over(path, json, createFolders = true)

  private def loadFromDisk(): Seq[Band] =
    try
      read[Seq[Band]](os.read(path))
    catch
      case _: Throwable =>
        Seq("20m")