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

import fdswarm.fx.contest.ContestManager
import fdswarm.fx.station.StationStore
import fdswarm.io.DirectoryProvider
import fdswarm.store.QsoStore
import fdswarm.model.Qso
import fdswarm.fx.contest.ContestType
import jakarta.inject.{Inject, Singleton}

@Singleton
final class ExportService @Inject()(
                                     qsoStore: QsoStore,
                                     directoryProvider: DirectoryProvider,
                                     contestManager: ContestManager,
                                     stationStore: StationStore
                                   ):

  enum ExportFormat(val extension: String, val description: String):
    case ADIF extends ExportFormat("adi", "ADIF Files (*.adi)")
    case CABRILLO extends ExportFormat("cbr", "Cabrillo Files (*.cbr)")
    case JSON extends ExportFormat("json", "JSON Files (*.json)")
    case ZIP  extends ExportFormat("zip", "Zip Files (*.zip)")

    override def toString: String = description

  def executeExport(path: os.Path, format: ExportFormat): Unit =
    format match
      case ExportFormat.ADIF =>
        val qsos = qsoStore.all
        val adif = AdifExporter.exportQsos(qsos)
        os.write.over(path, adif)
      case ExportFormat.CABRILLO =>
        val qsos = qsoStore.all
        val station = stationStore.station.value
        val contest = contestManager.config.contest
        val cabrillo = CabrilloExporter.exportQsos(qsos, station, contest)
        os.write.over(path, cabrillo)
      case ExportFormat.JSON =>
        val qsos = qsoStore.all
        val json = JsonExporter.exportQsos(qsos)
        os.write.over(path, json)
      case ExportFormat.ZIP =>
        ZipExporter.zipDirectory(directoryProvider(), path)
