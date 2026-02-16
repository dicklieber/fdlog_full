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

package fdswarm.api

import cask.*
import com.google.inject.{Inject, Singleton}
import com.typesafe.scalalogging.LazyLogging
import fdswarm.replication.NetworkConfig

@Singleton
class ApiService @Inject() (
    networkConfig: NetworkConfig,
    qsoRoutes: QsoRoutes
) extends Main with LazyLogging:
  override def port: Int = networkConfig.url.getPort
  override def host: String = "0.0.0.0"

  val allRoutes = Seq(
    SampleRoutes(),
    qsoRoutes
  )

  def start(): Unit =
    logger.info(s"Starting Cask API on http://$host:$port")
    main(Array.empty)
