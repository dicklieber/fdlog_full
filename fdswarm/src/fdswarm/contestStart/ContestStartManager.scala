package fdswarm.contestStart

import fdswarm.io.FileHelper
import fdswarm.logging.LazyStructuredLogging
import fdswarm.replication.{NodeStatusDispatcher, Service, StatusMessage, Transport}
import io.circe.generic.auto.{deriveDecoder, deriveEncoder}
import jakarta.inject.{Inject, Singleton}
import scalafx.application.Platform
import scalafx.beans.property.ObjectProperty

import java.time.Instant

@Singleton
final class ContestStartManager @Inject() (
    fileHelper: FileHelper,
    transport: Transport,
    nodeStatusDispatcher: NodeStatusDispatcher)
    extends LazyStructuredLogging:

  val contestStart: ObjectProperty[ContestStart] = ObjectProperty(ContestStart())
  private val fileName = "contestStart.json"

  contestStart.value = load()
  nodeStatusDispatcher.addListener(service = Service.ContestStart)((_, nextContestStart) =>
    Platform.runLater {
      update(nextContestStart = nextContestStart)
    }
  )
  nodeStatusDispatcher.addListener(service = Service.Status, singleListener = false)((_, statusMessage) =>
    Platform.runLater {
      updateFromStatus(statusMessage = statusMessage)
    }
  )

  def startContest(): Unit =
    val newContestStart = ContestStart(start = Instant.now())
    fileHelper.save(fileName, newContestStart)
    contestStart.value = newContestStart
    transport.send(Service.ContestStart, newContestStart)

  def update(nextContestStart: ContestStart): Unit =
    fileHelper.save(fileName, nextContestStart)
    contestStart.value = nextContestStart

  private def updateFromStatus(statusMessage: StatusMessage): Unit =
    update(nextContestStart = ContestStart(start = statusMessage.contestStart))

  private def load(): ContestStart =
    fileHelper.loadOrDefault[ContestStart](fileName)(ContestStart())
