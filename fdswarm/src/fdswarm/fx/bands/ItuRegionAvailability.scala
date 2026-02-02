package fdswarm.fx.bands

import upickle.default.*

enum BandClass derives ReadWriter:
  case LF, VLF, MF, HF, VHF, UHF, SHF, EHF

enum ItuRegion derives ReadWriter:
  case ALL, R1, R2, R3


/**
 * These are loaded from application.conf.
 *
 * @param bandName 30m, 40m, 80m, etc. Shown in UI.
 * @param startFrequencyHz used to map radio frequencies to [[HamBand]]
 * @param endFrequencyHz used to map radio frequencies to [[HamBand]]
 * @param bandClass common user-friendly name for band, e.g. "VHF"
 * @param regions where in the world this band is available.
 */
final case class HamBand(
                          bandName: String,
                          startFrequencyHz: Long,
                          endFrequencyHz: Long,
                          bandClass: BandClass,
                          regions: Set[ItuRegion] = Set(ItuRegion.ALL)
                        ) derives ReadWriter
