package fdswarm.fx.bands

enum BandClass:
  case HF, VHF, UHF, SHF, EHF, MF, LF, VLF

enum ItuRegion:
  case R1, R2, R3

sealed trait ItuRegionAvailability
object ItuRegionAvailability:
  case object AllRegions extends ItuRegionAvailability
  case class RegionsOnly(regions: Set[ItuRegion]) extends ItuRegionAvailability
  case object VariesByCountry extends ItuRegionAvailability

final case class  OMyHamBand(
                          bandName: String,
                          startFrequencyHz: Long,
                          endFrequencyHz: Long,
                          bandClass: BandClass,
                          ituRegionAvailability: ItuRegionAvailability
                        )