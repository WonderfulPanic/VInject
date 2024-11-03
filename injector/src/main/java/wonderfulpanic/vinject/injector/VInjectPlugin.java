/*
 * Copyright (C) 2024 WonderfulPanic
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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package wonderfulpanic.vinject.injector;

import org.slf4j.Logger;
import com.google.inject.Inject;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;

@Plugin(id = "vinject", name = "VInject", version = VInjectLoader.VERSION, authors = "WonderfulPanic")
public class VInjectPlugin {
	@Inject
	public VInjectPlugin(ProxyServer server, Logger log) throws Throwable {
		log.error("");
		log.error("VInject should not be loaded as plugin");
		log.error("Installation instructions can be found here:");
		log.error("https://github.com/WonderfulPanic/VInject");
		log.error("");
		server.shutdown();
	}
}