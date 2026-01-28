
package fdswarm.fx

import _root_.scalafx.beans.property.StringProperty
import fdswarm.fx.Station.Mode
import fdswarm.model.BandMode
import fdswarm.model.BandMode.Band
import fdswarm.model.Qso.CallSign
import jakarta.inject.*

import java.time.Instant


object Station:
  type Band = String
  type Mode = String

/**
 *
 * @param bandName band name limited to whats allow for contest
 * @param modeName CW,DI,PH
 * @param operator callSign of operator. must be a callSign
 * @param rig      using this rig free form.
 * @param antenna  and this antenna free form.
 */
case class Station(bandName: Band = "20m", modeName: Mode = "PH",
                   operator: CallSign = "", rig: String = "", antenna: String = "",
                   stamp: Instant = Instant.now()
                  ):

  lazy val bandMode: BandMode = BandMode(bandName, modeName)

  def isOk: Boolean = 

    operator.nonEmpty &&
      bandName.nonEmpty &&
      modeName.nonEmpty

  override def toString: String = s"$bandName $modeName $operator"


