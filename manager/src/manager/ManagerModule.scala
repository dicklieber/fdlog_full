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

package manager

import com.google.inject.AbstractModule
import com.typesafe.config.{Config, ConfigFactory}
import fdswarm.StartupInfo
import fdswarm.DirectoryProvider
import manager.io.ManagerDirectory
import net.codingwell.scalaguice.ScalaModule

class ManagerModule extends AbstractModule with ScalaModule {
  override def configure(): Unit = {
    bind[NodeConfigManager].asEagerSingleton()
    bind[Runner].asEagerSingleton()
    bind[DirectoryProvider].toInstance(new ManagerDirectory)
    bind[Config].toInstance(ConfigFactory.load())
    
    // Bind FX components from common
    bind[ModeCatalog].asEagerSingleton()
    bind[BandModeBuilder].asEagerSingleton()
    bind[StartupInfo].toInstance(new StartupInfo(Array.empty[String]))
    bind[AvailableBandsManager].asEagerSingleton()
    bind[AvailableModesManager].asEagerSingleton()
    bind[SelectedBandModeManager].asEagerSingleton()
    bind[BandModeMatrixPane].asEagerSingleton()
  }
}
