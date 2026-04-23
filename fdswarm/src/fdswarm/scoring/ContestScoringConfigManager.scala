package fdswarm.scoring

import fdswarm.logging.LazyStructuredLogging
import fdswarm.logging.Locus.Scoring
import io.circe.Encoder.AsArray.importedAsArrayEncoder
import io.circe.Encoder.AsObject.importedAsObjectEncoder
import io.circe.Encoder.AsRoot.importedAsRootEncoder
import io.circe.generic.auto.{deriveDecoder, deriveEncoder}
import io.circe.parser.decode
import io.circe.syntax.*
import jakarta.inject.*
import scalafx.beans.property.ObjectProperty

@Singleton
class ContestScoringConfigManager @Inject() (
                                              directoryProvider: fdswarm.DirectoryProvider
                                            ) extends LazyStructuredLogging(Scoring):

  private val configFile =
    directoryProvider() / "scoringConfig.json"

  val contestScoringConfigProperty: ObjectProperty[ContestScoringConfig] =
    ObjectProperty(load())

  def current: ContestScoringConfig =
    contestScoringConfigProperty.value

  def update(
              newConfig: ContestScoringConfig
            ): Unit =
    contestScoringConfigProperty.value = newConfig

  def save(): Unit =
    val json = current.asJson.spaces2
    os.write.over(configFile, json, createFolders = true)
    logger.info("savedContestScoringConfig" -> configFile.toString)

  private def load(): ContestScoringConfig =
    if os.exists(configFile) then
      decode[ContestScoringConfig](os.read(configFile)) match
        case Right(config) =>
          logger.info("loadedContestScoringConfig" -> configFile.toString)
          config

        case Left(error) =>
          logger.error(
            s"Failed to decode ContestScoringConfig from $configFile, using defaults",
            error
          )
          ContestScoringConfig()
    else
      logger.info(
        "contestScoringConfigMissingUsingDefaults" -> configFile.toString
      )
      ContestScoringConfig()

  contestScoringConfigProperty.onChange { (_, _, _) =>
    save()
  }