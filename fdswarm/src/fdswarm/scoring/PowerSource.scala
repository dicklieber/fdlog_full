package fdswarm.scoring

import io.circe.{Codec, Decoder, Encoder}

enum PowerSource derives CanEqual:
  case Battery
  case Solar
  case Commercial
  case Generator
  case Other

object PowerSource:
  given Codec[PowerSource] = Codec.from(
    Decoder.decodeString.emap { s =>
      PowerSource.values.find(_.toString == s) match
        case Some(ps) => Right(ps)
        case None     => Left(s"Invalid PowerSource: $s")
    },
    Encoder.encodeString.contramap(_.toString)
  )

case class ContestScoringConfig(
    powerWatts: Int = 100,
    powerSource: PowerSource = PowerSource.Battery,
    claimedObjectives: Set[String] = Set.empty,
    includeBonusesInLiveScore: Boolean = false
) derives Codec.AsObject