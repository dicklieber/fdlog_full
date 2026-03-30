package fdswarm.fx.contest

import com.google.inject.name.Named
import com.typesafe.scalalogging.LazyLogging
import fdswarm.io.DirectoryProvider
import io.circe.parser.decode
import io.circe.syntax.*
import jakarta.inject.{Inject, Singleton}
import scalafx.beans.property.{ReadOnlyObjectProperty, ReadOnlyObjectWrapper}

@Singleton
final class ContestConfigManager @Inject()(
                                            productionDirectory: DirectoryProvider,
                                            qsoStore: fdswarm.store.QsoStore,
                                            filenameStamp: fdswarm.util.FilenameStamp,
                                            @Named("fdswarm.contestChangeIgnoreStatusSec") ignoreStatusSec: Int
                                          ) extends LazyLogging:

  private var lastRestartTime: Long = 0L

  private val contestFile: os.Path =
    productionDirectory() / "contest.json"

  private var _contestConfig: ReadOnlyObjectWrapper[ContestConfig] | Null = load()

  def shouldIgnoreStatus: Boolean =
    val now = System.currentTimeMillis()
    (now - lastRestartTime) < (ignoreStatusSec * 1000L)

  @throws[IllegalStateException]("If not initialized")
  def contestConfigProperty: ReadOnlyObjectProperty[ContestConfig] =
    requireContestConfig().readOnlyProperty

  def setConfig(newConfig: ContestConfig): Unit =
    if _contestConfig == null then
      _contestConfig = ReadOnlyObjectWrapper(newConfig)
    else
      _contestConfig.nn.value = newConfig
    persist()

  /**
   * 1. Rename/archive qsos journal
   * 2. Clear in-memory stores
   * 3. Save new config
   */
  def handleRestartContest(newConfig: ContestConfig): Unit =
    lastRestartTime = System.currentTimeMillis()

    // archive + clear
    qsoStore.archiveAndClear()

    // update config
    if _contestConfig == null then
      _contestConfig = ReadOnlyObjectWrapper(newConfig)
    else
      _contestConfig.nn.value = newConfig

    // persist new config
    persist()

  private def load(): ReadOnlyObjectWrapper[ContestConfig] | Null =
    try
      (for
        file <- Option.when(os.exists(contestFile))(contestFile)
        jsonString = os.read(file)
        cfg <- decode[ContestConfig](jsonString) match
          case Right(value) =>
            Some(value)
          case Left(err) =>
            logger.error(
              s"""Failed to decode ContestConfig from $contestFile
                 |Error: ${err.getMessage}
                 |JSON:
                 |$jsonString
                 |""".stripMargin
            )
            None
      yield ReadOnlyObjectWrapper(cfg)).orNull
    catch
      case e: Throwable =>
        logger.error(s"Failed to load contest config from $contestFile", e)
        null

  private def persist(): Unit =
    try
      val cfg = requireContestConfig().value
      val json = cfg.asJson.spaces2
      val timestampedFile =
        productionDirectory() / s"${filenameStamp.build()}.contest.json"

      os.write.over(contestFile, json, createFolders = true)
      os.copy.over(contestFile, timestampedFile)

      logger.info(
        s"Persisted contest config to $contestFile and archived to $timestampedFile"
      )
    catch
      case e: Throwable =>
        logger.error(s"Failed to persist contest config to $contestFile", e)

  private def requireContestConfig(): ReadOnlyObjectWrapper[ContestConfig] =
    if _contestConfig == null then
      throw new IllegalStateException("contestConfig not initialized")
    _contestConfig.nn