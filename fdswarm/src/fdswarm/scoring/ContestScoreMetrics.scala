package fdswarm.scoring

enum ContestKind derives CanEqual:
  case ARRL
  case WFD

enum ArrlPowerSource derives CanEqual:
  case NonCommercial
  case CommercialOrGenerator

case class ArrlFieldDayConfig(
                               highestPowerWatts: BigDecimal,
                               powerSource: ArrlPowerSource
                             ):
  def powerMultiplier: Int =
    if highestPowerWatts <= BigDecimal(5) then
      powerSource match
        case ArrlPowerSource.NonCommercial         => 5
        case ArrlPowerSource.CommercialOrGenerator => 2
    else if highestPowerWatts <= BigDecimal(100) then
      2
    else
      1

case class ContestScoreConfig(
                               contestKind: ContestKind,
                               arrl: Option[ArrlFieldDayConfig] = None
                             )