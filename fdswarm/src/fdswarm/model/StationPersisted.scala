package fdswarm.model

import fdswarm.fx.bands.HamBand
import upickle.default.*
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
