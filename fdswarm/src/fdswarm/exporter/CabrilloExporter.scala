/*
 * Copyright (c) 2026. Dick Lieber, WA9NNN
 *
 * This program is free software: you can redistribute it and/or modify 
 * it under the terms of the GNU General Public License as published by 
 * the Free Software Foundation, either version 3 of the License, or    
 * (at your option) any later version.                                  
 *                                                                      
 * This program is distributed in the hope that it will be useful,      
 * but WITHOUT ANY WARRANTY; without even the implied warranty of       
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the        
 * GNU General Public License for more details.                         
 *                                                                      
 * You should have received a copy of the GNU General Public License    
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package fdswarm.exporter

import fdswarm.fx.contest.ContestType
import fdswarm.model.Qso
import fdswarm.model.Station
import java.time.format.DateTimeFormatter
import java.time.ZoneOffset

object CabrilloExporter:

  def exportQsos(qsos: Seq[Qso], station: fdswarm.model.Station, contest: ContestType): String =
    val sb = StringBuilder()
    sb.append("START-OF-LOG: 3.0\n")
    sb.append(s"CALLSIGN: ${station.operator.value}\n")
    sb.append(s"CONTEST: ${mapContest(contest)}\n")
    sb.append("CATEGORY-OPERATOR: MULTI-OP\n")
    sb.append("CATEGORY-STATION: FIXED\n")
    sb.append("CATEGORY-TRANSMITTER: MULTI-ONE\n")
    sb.append("CREATED-BY: FdSwarm\n")
    
    val sortedQsos = qsos.sortBy(_.stamp)
    sortedQsos.foreach { qso =>
      sb.append(toCabrilloRecord(qso))
      sb.append("\n")
    }
    
    sb.append("END-OF-LOG:\n")
    sb.toString()

  private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneOffset.UTC)
  private val timeFormatter = DateTimeFormatter.ofPattern("HHmm").withZone(ZoneOffset.UTC)

  private def mapContest(contest: ContestType): String =
    contest match
      case ContestType.WFD => "WFD"
      case ContestType.ARRL => "ARRL-FIELD-DAY"

  private def toCabrilloRecord(qso: Qso): String =
    val freq = fdswarm.model.BandMode.bandToFreq(qso.bandMode.band)
    val mode = qso.bandMode.cabMode
    val date = dateFormatter.format(qso.stamp)
    val time = timeFormatter.format(qso.stamp)
    val myCall = qso.qsoMetadata.station.operator.value
    val mySect = "XX" 
    val myCls = "1A"

    val hisCall = qso.callsign.value
    val hisClass = qso.contestClass
    val hisSect = qso.section

    f"QSO: $freq%5s $mode%2s $date $time $myCall%-12s $myCls%-3s $mySect%-3s $hisCall%-12s $hisClass%-3s $hisSect%-3s"
