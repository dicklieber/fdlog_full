package fdswarm.contestStart

import io.circe.Codec

import java.time.Instant

/**
 * Qsos with stamp older than this will be ignored
 * @param start when we started the contest.
 */
case class ContestStart(start: Instant = Instant.EPOCH) derives Codec.AsObject
