package fdswarm.contestStart

import io.circe.Codec

import java.time.Instant
import java.time.ZoneId

/**
  * Qsos with stamp older than this will be ignored
  * @param start when we started the contest.
  */
case class ContestStart(start: Instant = Instant.EPOCH) derives Codec.AsObject:
  override def toString: String =
    if isStarted then
      s"Contest started at ${start.atZone(ZoneId.systemDefault()).toLocalDateTime}"
    else
      "Contest not started"

  def isStarted: Boolean = start.isAfter(Instant.EPOCH)
