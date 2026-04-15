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

package fdswarm.replication.status

enum NodeDataField:
  case HostIp
  case Port
  case HostName
  case InstanceId
  case Received
  case IsLocal
  case QsoCount
  case Hash
  case StatusId
  case Operator
  case Band
  case Mode
  case BandMode
  case BandModeStamp
  case ContestType
  case ContestCallsign
  case ContestTransmitters
  case ContestClass
  case ContestSection
  case ContestStamp

  def label: String = this match
    case HostIp => "hostIp"
    case Port => "port"
    case HostName => "hostName"
    case InstanceId => "instanceId"
    case Received => "received"
    case IsLocal => "isLocal"
    case QsoCount => "qsoCount"
    case Hash => "hash"
    case StatusId => "statusId"
    case Operator => "operator"
    case Band => "band"
    case Mode => "mode"
    case BandMode => "bandMode"
    case BandModeStamp => "bandModeStamp"
    case ContestType => "contestType"
    case ContestCallsign => "contestCallsign"
    case ContestTransmitters => "contestTransmitters"
    case ContestClass => "contestClass"
    case ContestSection => "contestSection"
    case ContestStamp => "contestStamp"

object NodeDataField:
  val staticFields: Seq[NodeDataField] = Seq(
    NodeDataField.HostIp,
    NodeDataField.Port,
    NodeDataField.HostName,
    NodeDataField.InstanceId,
    NodeDataField.Received,
    NodeDataField.IsLocal,
    NodeDataField.QsoCount,
    NodeDataField.Hash,
    NodeDataField.StatusId,
    NodeDataField.Operator,
    NodeDataField.Band,
    NodeDataField.Mode,
    NodeDataField.BandMode,
    NodeDataField.BandModeStamp,
    NodeDataField.ContestType,
    NodeDataField.ContestCallsign,
    NodeDataField.ContestTransmitters,
    NodeDataField.ContestClass,
    NodeDataField.ContestSection,
    NodeDataField.ContestStamp
  )
