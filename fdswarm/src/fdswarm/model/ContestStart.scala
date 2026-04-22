package fdswarm.model

import fdswarm.io.DirectoryProvider
import fdswarm.logging.LazyStructuredLogging
import fdswarm.util.JavaTimeCirce.given
import io.circe.Codec
import io.circe.generic.auto.{deriveDecoder, deriveEncoder}
import io.circe.parser.decode
import io.circe.syntax.*
import jakarta.inject.{Inject, Singleton}
import scalafx.beans.property.ObjectProperty

import java.time.Instant

case class ContestStart(
  start: Instant = Instant.now
)derives Codec.AsObject

@Singleton
final class ContestStartStore @Inject() (
  directoryProvider: DirectoryProvider
) extends LazyStructuredLogging:

  private val file: os.Path =
    directoryProvider() / "contestStart.json"

  val contestStart: ObjectProperty[ContestStart] =
    ObjectProperty(
      load()
    )

  def update(
    nextContestStart: ContestStart
  ): Unit =
    contestStart.value = nextContestStart
    persist(
      contestStart = nextContestStart
    )

  private def persist(
    contestStart: ContestStart
  ): Unit =
    try
      os.write.over(
        file,
        contestStart.asJson.spaces2,
        createFolders = true
      )
    catch
      case error: Throwable =>
        logger.error(
          s"Failed to persist contest start to $file",
          error
        )

  private def load(): ContestStart =
    try
      if !os.exists(file) then ContestStart()
      else
        decode[ContestStart](
          os.read(file)
        ) match
          case Right(value) => value
          case Left(error) =>
            logger.error(
              s"Failed to decode contest start from $file, using defaults",
              error
            )
            ContestStart()
    catch
      case error: Throwable =>
        logger.error(
          s"Failed to load contest start from $file, using defaults",
          error
        )
        ContestStart()
