package fdswarm.scoring

import fdswarm.fx.contest.ContestType

object ContestScorerRegistry:

  def forType(ct: ContestType): ContestScorer =
    ct match
      case ContestType.WFD  => WfdScorer
      case ContestType.ARRL => ArrlFdScorer
      case ContestType.NONE => NoopScorer