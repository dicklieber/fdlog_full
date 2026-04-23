package fdswarm.contestStart

import fdswarm.io.FileHelper
import fdswarm.logging.LazyStructuredLogging
import io.circe.generic.auto.{deriveDecoder, deriveEncoder}
import jakarta.inject.{Inject, Singleton}
import scalafx.beans.property.ObjectProperty

@Singleton
final class ContestStartManager @Inject() (fileHelper: FileHelper) extends LazyStructuredLogging:

  val contestStart: ObjectProperty[ContestStart] = ObjectProperty(load())
  private lazy val fileName = "contestStart.json"

  def update(nextContestStart: ContestStart): Unit =
    contestStart.value = nextContestStart
    fileHelper.save(fileName, nextContestStart)

  private def load(): ContestStart =
    fileHelper.loadOrDefault[ContestStart](fileName)(ContestStart())
