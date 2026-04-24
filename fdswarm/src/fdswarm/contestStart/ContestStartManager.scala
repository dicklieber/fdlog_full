package fdswarm.contestStart

import fdswarm.io.FileHelper
import fdswarm.logging.LazyStructuredLogging
import fdswarm.replication.{Service, Transport}
import io.circe.generic.auto.{deriveDecoder, deriveEncoder}
import jakarta.inject.{Inject, Singleton}
import scalafx.beans.property.ObjectProperty

import java.time.Instant

@Singleton
final class ContestStartManager @Inject() (fileHelper: FileHelper, transport: Transport) extends LazyStructuredLogging:

  private lazy val fileName = "contestStart.json"
  val contestStart: ObjectProperty[ContestStart] = ObjectProperty(load())

  def startContest(): Unit =
    val newContestStart = ContestStart(start = Instant.now())
    update(newContestStart)
    transport.send(Service.ContestStart, newContestStart)

  def update(nextContestStart: ContestStart): Unit =
    fileHelper.save(fileName, nextContestStart)
    contestStart.value = nextContestStart

  private def load(): ContestStart = fileHelper.loadOrDefault[ContestStart](fileName)(ContestStart())
