package fdswarm.scoring

case class ContestScoreSnapshot(
                                 totalScore: Int,
                                 rawPoints: Int,
                                 multiplier: Double,
                                 totalQsos: Int,
                                 byMode: Map[String, Int],
                                 byBand: Map[String, Int],
                                 breakdown: Map[String, Double]
                               )