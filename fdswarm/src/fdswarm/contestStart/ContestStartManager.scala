package fdswarm.contestStart

import fdswarm.io.FileHelper
import fdswarm.logging.LazyStructuredLogging
import fdswarm.replication.{Service, Transport}
import io.circe.generic.auto.{deriveDecoder, deriveEncoder}
import jakarta.inject.{Inject, Singleton}
import scalafx.beans.property.ObjectProperty

import java.time.Instant

@Singleton
final class ContestStartManager @Inject() (fileHelper: FileHelper, transport: Transport)
    extends LazyStructuredLogging:

  val contestStart: ObjectProperty[ContestStart] = ObjectProperty(ContestStart())
  private val fileName = "contestStart.json"

  contestStart.value = load()

  def startContest(): Unit =
    val newContestStart = ContestStart(start = Instant.now())
    fileHelper.save(fileName, newContestStart)
    contestStart.value = newContestStart
    transport.send(Service.ContestStart, newContestStart)

  def update(nextContestStart: ContestStart): Unit =
    fileHelper.save(fileName, nextContestStart)
    contestStart.value = nextContestStart

  private def load(): ContestStart =
    fileHelper.loadOrDefault[ContestStart](fileName)(ContestStart())
