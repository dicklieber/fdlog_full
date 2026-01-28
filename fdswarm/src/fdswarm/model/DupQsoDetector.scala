package fdswarm.model

import fdswarm.model.BandMode
import fdswarm.store.QsoStore
import jakarta.inject.*

import scala.concurrent.Future

@Singleton
class DupQsoDetector @Inject()(qsoStore:QsoStore, station: Station):

  def apply(startOfCallsign: String): Seq[Qso] =
    qsoStore.potentialDups(startOfCallsign, station.bandMode)
