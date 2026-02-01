package fdswarm.model

import upickle.default.*
import fdswarm.fx.bands.HamBand
import fdswarm.fx.caseForm.ChoiceField

/** Persistable representation of Station.
 *
 * IMPORTANT:
 *   Station (UI model) currently contains ChoiceField[HamBand] which is not directly serializable.
 *   This DTO strips UI wrappers and stores the selected HamBand value.
 */
final case class StationPersisted(
                                   bandName: String,
                                   mode:     BandMode.Mode,
                                   rig:      String,
                                   antenna:  String,
                                   operator: Callsign
                                 ) derives ReadWriter

object StationPersisted:

  /** Extract the selected HamBand from a ChoiceField.
   *
   * Based on your compilation error, `cf.value` is already a HamBand.
   * If your ChoiceField changes shape later, this is the only place to update.
   */
  private def selectedHamBand(cf: ChoiceField[HamBand]): HamBand =
    cf.value

  /** Convert UI Station -> persistable StationPersisted */
  def fromStation(s: Station): StationPersisted =
    StationPersisted(
      bandName = selectedHamBand(s.band).bandName,
      mode     = s.modeName,
      rig      = s.rig,
      antenna  = s.antenna,
      operator = s.operator
    )

  /** Convert persistable StationPersisted -> UI Station.
   *
   * You MUST provide a function that can build the ChoiceField[HamBand] for the UI.
   * Typically you already have something like `hamBandChoice(current: HamBand): ChoiceField[HamBand]`.
   */
  def toStation(
                 p: StationPersisted,
                 makeBandChoice: HamBand => ChoiceField[HamBand],
                 lookupBand: String => HamBand
               ): Station =
    Station(
      band     = makeBandChoice(lookupBand(p.bandName)),
      modeName = p.mode,
      rig      = p.rig,
      antenna  = p.antenna,
      operator = p.operator
    )