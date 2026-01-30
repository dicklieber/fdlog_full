package fdswarm.fx.bands

/**
 * Data for a ham band, loaded from application.conf via HamBandCatalog.
 *
 * Keys expected in config:
 *  - bandName
 *  - startFrequencyHz
 *  - endFrequencyHz
 *  - bandClass
 *  - ituRegionAvailability { ... }  (optional)
 */
final case class HamBand(
                          bandName: String,
                          startFrequencyHz: Long,
                          endFrequencyHz: Long,
                          bandClass: BandClass,
                          ituRegionAvailability: ItuRegionAvailability
                        )