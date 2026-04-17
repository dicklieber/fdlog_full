package fdswarm.scoring

import fdswarm.fx.contest.ContestType
import jakarta.inject.{Inject, Singleton}

@Singleton
class ContestScorerRegistry @Inject() (
                                        wfdScorer: WfdScorer,
                                        arrlFdScorer: ArrlFdScorer
                                      ):

  def forType(ct: ContestType): ContestScorer =
    ct match
      case ContestType.WFD  => wfdScorer
      case ContestType.ARRL => arrlFdScorer
      case ContestType.NONE => NoopScorer