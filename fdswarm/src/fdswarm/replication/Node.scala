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

package fdswarm.replication

import fdswarm.fx.contest.ContestConfig
import fdswarm.model.Callsign
import io.circe.{Codec, Decoder, Encoder}

import java.net.{URI, URL}

/**
 * 
 * @param url where to find this node.
 * @param contestConfig details about contest.
 * @param ourStation who is participating in this contest.
 */
given Encoder[URL] = Encoder.encodeString.contramap(_.toString)
given Decoder[URL] = Decoder.decodeString.emap(str => scala.util.Try(URI.create(str).toURL).toEither.left.map(_.getMessage))

case class Node(url: URL,
                contestConfig: ContestConfig,
                ourStation: Callsign) derives Codec.AsObject
