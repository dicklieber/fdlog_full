package fdswarm.model

import upickle.ReadWriter

// Scala 3: Worldwide (ITU) amateur bands as an enum with bandClass + ITU region availability.
// Notes:
// - "Availability" here is a *practical* label (since national rules vary).
// - 60m is especially country-specific (often channelized), so it's marked "VariesByCountry".
// - 4m / 1.25m / 33cm are not universally available worldwide, so they're marked region-limited.

enum BandClass:
  case LF, MF, HF, VHF, UHF, SHF, EHF

enum ItuRegion:
  case R1, R2, R3 // 1=Europe/Africa/Middle East, 2=Americas, 3=Asia-Pacific

enum ItuRegionAvailability:
  /** Commonly available across all 3 ITU regions (though national rules still apply). */
  case AllRegions
  /** Mostly available/used in specific regions (still country-dependent within regions). */
  case RegionsOnly(regions: Set[ItuRegion])
  /** Strongly varies by country (channelization, allocations, license privileges, etc.). */
  case VariesByCountry

enum HamBand(
              val bandName: String,
              val startFrequencyHz: Long,
              val endFrequencyHz: Long,
              val bandClass: BandClass,
              val ituRegionAvailability: ItuRegionAvailability
            ) derives ReadWriter:
  // LF
  case B2200m extends HamBand("2200m", 135_700L, 137_800L, BandClass.LF, ItuRegionAvailability.AllRegions)

  // MF
  case B630m  extends HamBand("630m", 472_000L, 479_000L, BandClass.MF, ItuRegionAvailability.AllRegions)
  case B160m  extends HamBand("160m", 1_800_000L, 2_000_000L, BandClass.MF, ItuRegionAvailability.AllRegions)

  // HF
  case B80m   extends HamBand("80m", 3_500_000L, 4_000_000L, BandClass.HF, ItuRegionAvailability.AllRegions)
  case B60m   extends HamBand("60m", 5_250_000L, 5_450_000L, BandClass.HF, ItuRegionAvailability.VariesByCountry)
  case B40m   extends HamBand("40m", 7_000_000L, 7_300_000L, BandClass.HF, ItuRegionAvailability.AllRegions)
  case B30m   extends HamBand("30m", 10_100_000L, 10_150_000L, BandClass.HF, ItuRegionAvailability.AllRegions)
  case B20m   extends HamBand("20m", 14_000_000L, 14_350_000L, BandClass.HF, ItuRegionAvailability.AllRegions)
  case B17m   extends HamBand("17m", 18_068_000L, 18_168_000L, BandClass.HF, ItuRegionAvailability.AllRegions)
  case B15m   extends HamBand("15m", 21_000_000L, 21_450_000L, BandClass.HF, ItuRegionAvailability.AllRegions)
  case B12m   extends HamBand("12m", 24_890_000L, 24_990_000L, BandClass.HF, ItuRegionAvailability.AllRegions)
  case B10m   extends HamBand("10m", 28_000_000L, 29_700_000L, BandClass.HF, ItuRegionAvailability.AllRegions)

  // VHF
  case B6m    extends HamBand("6m", 50_000_000L, 54_000_000L, BandClass.VHF, ItuRegionAvailability.AllRegions)
  case B4m    extends HamBand("4m", 70_000_000L, 70_500_000L, BandClass.VHF, ItuRegionAvailability.RegionsOnly(Set(ItuRegion.R1, ItuRegion.R3)))
  case B2m    extends HamBand("2m", 144_000_000L, 148_000_000L, BandClass.VHF, ItuRegionAvailability.AllRegions)

  // UHF
  case B1_25m extends HamBand("1.25m", 219_000_000L, 225_000_000L, BandClass.UHF, ItuRegionAvailability.RegionsOnly(Set(ItuRegion.R2)))
  case B70cm  extends HamBand("70cm", 420_000_000L, 450_000_000L, BandClass.UHF, ItuRegionAvailability.AllRegions)
  case B33cm  extends HamBand("33cm", 902_000_000L, 928_000_000L, BandClass.UHF, ItuRegionAvailability.RegionsOnly(Set(ItuRegion.R2)))
  case B23cm  extends HamBand("23cm", 1_240_000_000L, 1_300_000_000L, BandClass.UHF, ItuRegionAvailability.AllRegions)

  // SHF
  case B13cm  extends HamBand("13cm", 2_300_000_000L, 2_450_000_000L, BandClass.SHF, ItuRegionAvailability.AllRegions)
  case B9cm   extends HamBand("9cm", 3_300_000_000L, 3_500_000_000L, BandClass.SHF, ItuRegionAvailability.AllRegions)
  case B6cm   extends HamBand("6cm", 5_650_000_000L, 5_925_000_000L, BandClass.SHF, ItuRegionAvailability.AllRegions)
  case B3cm   extends HamBand("3cm", 10_000_000_000L, 10_500_000_000L, BandClass.SHF, ItuRegionAvailability.AllRegions)
  case B1_25cm extends HamBand("1.25cm", 24_000_000_000L, 24_250_000_000L, BandClass.SHF, ItuRegionAvailability.AllRegions)

  // EHF
  case B6mm   extends HamBand("6mm", 47_000_000_000L, 47_200_000_000L, BandClass.EHF, ItuRegionAvailability.AllRegions)
  case B4mm   extends HamBand("4mm", 75_500_000_000L, 81_000_000_000L, BandClass.EHF, ItuRegionAvailability.AllRegions)
  case B2_5mm extends HamBand("2.5mm", 119_000_000_000L, 123_000_000_000L, BandClass.EHF, ItuRegionAvailability.AllRegions)
  case B2mm   extends HamBand("2mm", 134_000_000_000L, 141_000_000_000L, BandClass.EHF, ItuRegionAvailability.AllRegions)
  case B1mm   extends HamBand("1mm", 241_000_000_000L, 250_000_000_000L, BandClass.EHF, ItuRegionAvailability.AllRegions)

object HamBand:
  /** Convenience list, in declaration order */
  val all: List[HamBand] = HamBand.values.toList

  /** Find by canonical bandName (e.g., "40m", "2m") */
  def byName(name: String): Option[HamBand] =
    val n = name.trim.toLowerCase
    all.find(_.bandName.toLowerCase == n)

  /** Find the band that contains a given frequency in Hz */
  def containing(freqHz: Long): Option[HamBand] =
    all.find(b => freqHz >= b.startFrequencyHz && freqHz <= b.endFrequencyHz)