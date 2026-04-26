package fdswarm.scoring

import fdswarm.io.FileHelper
import fdswarm.logging.LazyStructuredLogging
import fdswarm.logging.Locus.Scoring
import io.circe.Encoder.AsObject.importedAsObjectEncoder
import io.circe.generic.auto.{deriveDecoder, deriveEncoder}
import jakarta.inject.*
import scalafx.beans.property.ObjectProperty

@Singleton
class ContestScoringConfigManager @Inject() (fileHelper: FileHelper) extends LazyStructuredLogging(Scoring):

  private val file = "scoringConfig.json"

  val contestScoringConfigProperty: ObjectProperty[ContestScoringConfig] =
    ObjectProperty(load())

  def update(
      newConfig: ContestScoringConfig
  ): Unit =
    contestScoringConfigProperty.value = newConfig

  def save(): Unit =
    fileHelper.save(file, current)

  def current: ContestScoringConfig =
    contestScoringConfigProperty.value

  private def load(): ContestScoringConfig =
    fileHelper.loadOrDefault(file)(ContestScoringConfig())

  contestScoringConfigProperty.onChange { (_, _, _) =>
    save()
  }
