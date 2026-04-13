package fdswarm.fx.contest

import com.google.inject.name.Named
import fdswarm.logging.LazyStructuredLogging
import fdswarm.io.DirectoryProvider
import fdswarm.logging.StructuredLogger
import fdswarm.model.Callsign
import fdswarm.replication.NodeStatus
import io.circe.parser.decode
import io.circe.syntax.*
import jakarta.inject.{Inject, Provider, Singleton}
import fdswarm.logging.LazyStructuredLogging
import scalafx.beans.property.{ObjectProperty, ReadOnlyBooleanProperty, ReadOnlyBooleanWrapper}

@Singleton
final class ContestConfigManager @Inject()(
                                            productionDirectory: DirectoryProvider,
                                          qsoStoreProvider: Provider[fdswarm.store.QsoStore],
                                          filenameStamp: fdswarm.util.FilenameStamp,
                                          @Named("fdswarm.contestChangeIgnoreStatusSec") ignoreStatusSec: Int
                                          ) extends ContestConfigFields with LazyStructuredLogging:
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
    if newConfig.contestType == ContestType.NONE then
      removePersistedConfig()
    else
      persist()

  def updateFromNodeStatus(
                            nodeStatus: NodeStatus
                          ): Unit =
    val receivedConfig = nodeStatus.statusMessage.contestConfig
    if receivedConfig.contestType == ContestType.NONE then
      return

    val localConfig = _contestConfig.value
    if localConfig.contestType == ContestType.NONE then
      logger.info(
        "Applying received contest config because local contest type is NONE.",
        ("contestType", receivedConfig.contestType.toString),
        ("receivedStamp", receivedConfig.stamp.toString)
      )
      setConfig(
        receivedConfig
      )
      return

    if receivedConfig.stamp.isBefore(
        localConfig.stamp
      ) then
      logger.info(
        "Received Config",
        ("contestType", receivedConfig.contestType.toString),
        ("receivedStamp", receivedConfig.stamp.toString)
      )
      setConfig(
        receivedConfig
      )

  def clearContestConfig(): Unit =
    setConfig(
      ContestConfig.noContest
    )

  /**
   * 1. Rename/archive qsos journal
   * 2. Clear in-memory stores
   * 3. Save new config
   */
  def handleRestartContest(
                           newConfig: ContestConfig
                         ): Unit =
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
            logger.debug(s"Loading contest config from", "file" -> file.toString())
            val jsonString = os.read(
              file
            )
            decode[ContestConfig](
              jsonString
            ) match
            case Right(value) =>
              value
            case Left(err) =>
              logger.error("Failed to decode ContestConfig", err, "json" -> jsonString)
              ContestConfig.noContest
        )
        .getOrElse(
          ContestConfig.noContest
        )
    catch
      case e: Throwable =>
        logger.error(
          "Failed to load contest", e
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

  private def removePersistedConfig(): Unit =
    try
      if os.exists(
          contestFile
        ) then
        os.remove(
          contestFile
        )
        logger.info(
          s"Removed contest config file $contestFile"
        )
    catch
      case e: Throwable =>
        logger.error(
          s"Failed to remove contest config file $contestFile",
          e
        )
