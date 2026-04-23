package fdswarm.fx.contest

import com.google.inject.name.Named
import fdswarm.logging.LazyStructuredLogging
import fdswarm.logging.Locus.Scoring
import fdswarm.model.Callsign
import fdswarm.replication.{NodeStatusDispatcher, Service}
import io.circe.generic.auto.{deriveDecoder, deriveEncoder}
import io.circe.parser.decode
import io.circe.syntax.*
import jakarta.inject.{Inject, Singleton}
import scalafx.application.Platform
import scalafx.beans.property.{ObjectProperty, ReadOnlyBooleanProperty, ReadOnlyBooleanWrapper}

@Singleton
final class ContestConfigManager @Inject() (
    productionDirectory: fdswarm.DirectoryProvider,
    filenameStamp: fdswarm.util.FilenameStamp,
    nodeStatusDispatcher: NodeStatusDispatcher,
    @Named("fdswarm.contestChangeIgnoreStatusSec")
    ignoreStatusSec: Int)
    extends ContestConfigFields with LazyStructuredLogging(Scoring):
  lazy val hasConfiguration: ReadOnlyBooleanProperty = _hasConfiguration.readOnlyProperty
  private val contestFile: os.Path = productionDirectory() / "contest.json"
  private val _contestConfig: ObjectProperty[ContestConfig] = ObjectProperty(load())
  private val _hasConfiguration =
    new ReadOnlyBooleanWrapper(this, "hasConfiguration", _contestConfig.value.contestType != ContestType.NONE)

// These override methods expose the current value of the contestConfigProperty
  override def contestType: ContestType = contestConfigProperty.value.contestType

  override def ourCallsign: Callsign = contestConfigProperty.value.ourCallsign

  def contestConfigProperty: ObjectProperty[ContestConfig] = _contestConfig

  override def transmitters: Int = contestConfigProperty.value.transmitters

  override def ourClass: String = contestConfigProperty.value.ourClass

  _contestConfig.onChange((_, _, config) => _hasConfiguration.value = config.contestType != ContestType.NONE)

  nodeStatusDispatcher
    .addListener(service = Service.SyncContest)((_, newConfig) =>
      handleContestConfigUpdate(newConfig = newConfig))

  override def ourSection: String = contestConfigProperty.value.ourSection

  def clearContestConfig(): Unit = setConfig(ContestConfig.noContest)

  def setConfig(newConfig: ContestConfig): Unit =
    _contestConfig.value = newConfig
    if newConfig.contestType == ContestType.NONE then removePersistedConfig() else persist()

  private def persist(): Unit =
    try
      val cfg = _contestConfig.value
      val json = cfg.asJson.spaces2
      val timestampedFile = productionDirectory() / s"${filenameStamp.build()}.contest.json"

      os.write.over(contestFile, json, createFolders = true)
      os.copy.over(contestFile, timestampedFile)

      logger.info(s"Persisted contest config to $contestFile and archived to $timestampedFile")
    catch case e: Throwable => logger.error(s"Failed to persist contest config to $contestFile", e)

  private def removePersistedConfig(): Unit =
    try
      if os.exists(contestFile) then
        os.remove(contestFile)
        logger.info(s"Removed contest config file $contestFile")
    catch case e: Throwable => logger.error(s"Failed to remove contest config file $contestFile", e)

  private def handleContestConfigUpdate(newConfig: ContestConfig): Unit =
    logger.debug("Received Contest", "ContestConfig" -> newConfig)
    Platform.runLater { setConfig(newConfig) }

  private def load(): ContestConfig =
    try
      Option.when(os.exists(contestFile))(contestFile).map(file =>
        logger.debug(s"Loading contest config from", "file" -> file.toString())
        val jsonString = os.read(file)
        decode[ContestConfig](jsonString) match
          case Right(value) => value
          case Left(err) =>
            logger.error("Failed to decode ContestConfig", err, "json" -> jsonString)
            ContestConfig.noContest).getOrElse(ContestConfig.noContest)
    catch
      case e: Throwable =>
        logger.error("Failed to load contest", e)
        ContestConfig.noContest
