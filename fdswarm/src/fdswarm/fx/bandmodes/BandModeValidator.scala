package fdswarm.fx.bandmodes

import jakarta.inject.{Inject, Singleton}

/** Simple validation helper for user-chosen band/mode pairs. */
@Singleton
final class BandModeValidator @Inject() (store: BandModeStore):

  /** Returns None if OK, else an error message. */
  def validate(bm: BandMode): Option[String] =
    val state = store.currentBandMode

    if !state.modes.contains(bm.mode) then
      Some(s"Mode '${bm.mode}' is not selected")
    else if !state.bands.contains(bm.band) then
      Some(s"Band '${bm.band}' is not selected")
    else if !store.isEnabled(bm.mode, bm.band) then
      Some(s"Band/mode '${bm.band}' / '${bm.mode}' is not enabled")
    else
      None