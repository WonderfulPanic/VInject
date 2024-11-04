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

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.zip.ZipFile;
import org.objectweb.asm.tree.ClassNode;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import wonderfulpanic.vinject.injector.util.ResourceUtil;

public class VInjectPluginManager {
	private final List<Plugin> plugins = new ArrayList<>();
	public List<Plugin> getPlugins() {
		return plugins;
	}
	public void loadPlugins(VInjectLoader loader) {
		File pluginsFolder = new File("plugins");
		if (!pluginsFolder.exists() || !pluginsFolder.isDirectory())
			return;
		Map<String, Plugin> pluginMap = new HashMap<>();
		for (File pluginFile : pluginsFolder.listFiles()) {
			if (!pluginFile.isFile() || !pluginFile.getName().endsWith(".jar"))
				continue;
			try (ZipFile zip = new ZipFile(pluginFile)) {
				loadPlugin(loader, pluginMap, pluginFile, zip);
			} catch (Throwable t) {
				throw new InternalError("Caught exception loading plugin " + pluginFile.getName(), t);
			}
		}
		plugins.addAll(pluginMap.values());
	}
	public Plugin loadPlugin(VInjectLoader loader, Map<String, Plugin> pluginMap, File pluginFile, ZipFile zip)
		throws IOException, ClassNotFoundException {
		JsonObject velocity = ResourceUtil.getJson(zip, "velocity-plugin.json");
		String pluginId = null;
		if (velocity != null && velocity.has("id"))
			pluginId = velocity.get("id").getAsString();
		JsonObject vinject = ResourceUtil.getJson(zip, "vinject-plugin.json");
		if (pluginId == null) {
			if (vinject == null)
				return null;
			else
				throw new IllegalArgumentException(
					"Found vinject-plugin.json but velocity-plugin.json does not contain plugin id");
		}
		Plugin plugin = new Plugin(pluginId, pluginFile);
		if (pluginMap.putIfAbsent(pluginId, plugin) != null) {
			System.err.printf("[VInject] Plugin %s (%s) already exists%n", pluginId, pluginFile);
			return null;
		}
		if (vinject == null)
			return plugin;
		String pack = vinject.has("package") ?
			ResourceUtil.asValidPath(vinject.get("package").getAsString()) + '/' : "";
		Boolean required = vinject.has("required") ?
			Boolean.valueOf(vinject.get("required").getAsBoolean()) : Boolean.TRUE;
		for (JsonElement name : vinject.get("injects").getAsJsonArray())
			loadInjector(loader, plugin, required,
				ResourceUtil.loadNode(zip, pack + ResourceUtil.asValidPath(name.getAsString())));
		return plugin;
	}
	public void loadInjector(VInjectLoader loader, Plugin plugin, Boolean required, ClassNode injector) {
		plugin.getInjectors().add(injector);
		loader.loadInjector(plugin, required, injector);
	}
	public MethodHandle initPlugins(VInjectLoader loader, VInjectClassLoader classLoader, Class<?> pcl)
		throws Throwable {
		Lookup lookup = MethodHandles.privateLookupIn(pcl, MethodHandles.lookup());
		lookup.findStaticSetter(pcl, "vinject$velocity", Function.class)
			.invokeExact((Function<String, Class<?>>) name -> classLoader.loadClassForPlugin(loader, name));
		MethodHandle constructor = lookup.findConstructor(pcl, MethodType.methodType(void.class, URL[].class))
			.asType(MethodType.methodType(Object.class, URL[].class));
		MethodHandle add = lookup.findVirtual(pcl, "addToClassloaders", MethodType.methodType(void.class))
			.asType(MethodType.methodType(void.class, Object.class));
		for (Plugin plugin : plugins) {
			Object instance = constructor.invokeExact(new URL[]{plugin.getPluginFile().toURI().toURL()});
			plugin.setClassLoader((ClassLoader) instance);
			add.invokeExact(instance);
		}
		return constructor;
	}
}