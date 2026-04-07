package fdswarm.fx.contest

import com.google.inject.name.Named
import com.typesafe.scalalogging.LazyLogging
import fdswarm.io.DirectoryProvider
import fdswarm.model.Callsign
import io.circe.parser.decode
import io.circe.syntax.*
import jakarta.inject.{Inject, Singleton, Provider}
import scalafx.beans.property.{ObjectProperty, ReadOnlyBooleanProperty, ReadOnlyBooleanWrapper}

@Singleton
final class ContestConfigManager @Inject()(
                                            productionDirectory: DirectoryProvider,
                                            qsoStoreProvider: Provider[fdswarm.store.QsoStore],
                                            filenameStamp: fdswarm.util.FilenameStamp,
                                            @Named("fdswarm.contestChangeIgnoreStatusSec") ignoreStatusSec: Int
                                          ) extends ContestConfigFields with LazyLogging:
  private var onConfigSetListeners: Vector[ContestConfig => Unit] = Vector.empty

  private def qsoStore: fdswarm.store.QsoStore = qsoStoreProvider.get()
// These override methods expose the current value of the contestConfigProperty
  override def contestType: ContestType =
    contestConfigProperty.value.contestType

  override def ourCallsign: Callsign =
    contestConfigProperty.value.ourCallsign

  override def transmitters: Int =
    contestConfigProperty.value.transmitters

  override def ourClass: String =
    contestConfigProperty.value.ourClass

  override def ourSection: String =
    contestConfigProperty.value.ourSection


  private var lastRestartTime: Long = 0L

  private val contestFile: os.Path =
    productionDirectory() / "contest.json"

  private val _contestConfig: ObjectProperty[ContestConfig] =
    ObjectProperty(
      load()
    )

  private val _hasConfiguration = new ReadOnlyBooleanWrapper(
    this,
    "hasConfiguration",
    _contestConfig.value.contestType != ContestType.NONE
  )

  _contestConfig.onChange(
    (_, _, config) =>
      _hasConfiguration.value = config.contestType != ContestType.NONE
  )

  val hasConfiguration: ReadOnlyBooleanProperty = _hasConfiguration.readOnlyProperty

  def onConfigSet(
                   listener: ContestConfig => Unit
                 ): Unit =
    onConfigSetListeners :+= listener

  def shouldIgnoreStatus: Boolean =
    val now = System.currentTimeMillis()
    (now - lastRestartTime) < (ignoreStatusSec * 1000L)

  def contestConfigOption: Option[ContestConfig] =
    Some(
      _contestConfig.value
    )

  def contestConfigProperty: ObjectProperty[ContestConfig] =
    _contestConfig

  def setConfig(
                 newConfig: ContestConfig
               ): Unit =
    _contestConfig.value = newConfig
    persist()
    notifyConfigSet(
      newConfig
    )

  /**
   * 1. Rename/archive qsos journal
   * 2. Clear in-memory stores
   * 3. Save new config
   */
  def handleRestartContest(
                           newConfig: ContestConfig
                         ): Unit =
    lastRestartTime = System.currentTimeMillis()

    // archive + clear
    qsoStore.archiveAndClear()

    // update config + persist + notify
    setConfig(
      newConfig
    )

  private def load(): ContestConfig =
    try
      Option
        .when(
          os.exists(
            contestFile
          )
        )(
          contestFile
        )
        .map(
          file =>
            val jsonString = os.read(
              file
            )
            decode[ContestConfig](
              jsonString
            ) match
            case Right(value) =>
              value
            case Left(err) =>
              logger.error(
                s"""Failed to decode ContestConfig from $contestFile
                   |Error: ${err.getMessage}
                   |JSON:
                   |$jsonString
                   |""".stripMargin
              )
              ContestConfig.noContest
        )
        .getOrElse(
          ContestConfig.noContest
        )
    catch
      case e: Throwable =>
        logger.error(
          s"Failed to load contest config from $contestFile",
          e
        )
        ContestConfig.noContest

  private def persist(): Unit =
    try
      val cfg = _contestConfig.value
      val json = cfg.asJson.spaces2
      val timestampedFile =
        productionDirectory() / s"${filenameStamp.build()}.contest.json"

      os.write.over(
        contestFile,
        json,
        createFolders = true
      )
      os.copy.over(
        contestFile,
        timestampedFile
      )

      logger.info(
        s"Persisted contest config to $contestFile and archived to $timestampedFile"
      )
    catch
      case e: Throwable =>
        logger.error(
          s"Failed to persist contest config to $contestFile",
          e
        )

  private def notifyConfigSet(
                               config: ContestConfig
                             ): Unit =
    onConfigSetListeners.foreach { listener =>
      try
        listener(
          config
        )
      catch
        case e: Throwable =>
          logger.error(
            "ContestConfigManager.onConfigSet listener failed",
            e
          )
    }
