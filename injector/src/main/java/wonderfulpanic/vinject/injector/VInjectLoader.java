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

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import org.objectweb.asm.tree.ClassNode;
import wonderfulpanic.vinject.injector.injectors.JPLInjector;
import wonderfulpanic.vinject.injector.injectors.PCLInjector;
import wonderfulpanic.vinject.injector.util.InjectUtil;
import wonderfulpanic.vinject.injector.util.ResourceUtil;

public class VInjectLoader {
	public static final boolean EXPORT = Boolean.getBoolean("vinject.export");
	public static final boolean DEBUG = Boolean.getBoolean("vinject.debug");
	public static final boolean FORCE_LOAD = Boolean.getBoolean("vinject.forceload");
	public static final String VERSION = "1.0.0";
	public static final PrintStream out = DEBUG ? System.out : null;
	private final VInjectClassLoader loader;
	private final VInjectPluginManager pluginManager = new VInjectPluginManager();
	private final Map<String, List<ClassNode>> injectorsByClassName = new HashMap<>();
	private final Set<ClassNode> unrequired = new HashSet<>();
	private MethodHandle classLoaderConstructor;
	public VInjectLoader(VInjectClassLoader loader) {
		this.loader = loader;
	}
	public void load(String[] args) throws Throwable {
		pluginManager.loadPlugins(this);
		classLoaderConstructor = pluginManager.initPlugins(this, loader, loadPluginClassLoader());
		loader.setTransformer(name -> {
			String path = ResourceUtil.asPath(name) + ".class";
			if (loader.getParent().getResource(path) != null)
				return null;
			try (InputStream in = loader.getResourceAsStream(path)) {
				if (in != null) {
					if (injectorsByClassName.containsKey(name))
						return loader.defineClass(applyInjectors(ResourceUtil.getNode(in)));
					else
						return null;
				}
			} catch (IOException e) {
				throw new ClassNotFoundException(name, e);
			}
			for (Plugin plugin : pluginManager.getPlugins()) {
				try (InputStream in = plugin.getClassLoader().getResourceAsStream(path)) {
					if (in == null)
						continue;
					if (DEBUG)
						out.printf("[VInject] Found class %s for velocity in plugin: %s%n", name, plugin.id());
					if (injectorsByClassName.containsKey(name))
						return plugin.defineClass(applyInjectors(ResourceUtil.getNode(in)));
					else
						return plugin.getClassLoader().loadClass(name);
				} catch (Throwable e) {
					throw new ClassNotFoundException(name, e);
				}
			}
			return null;
		});
		hookJPL(loadJavaPluginLoader());
		if (FORCE_LOAD) {
			boolean errored = false;
			if (DEBUG)
				out.println("[VInject] Force loading all injectors");
			for (String name : injectorsByClassName.keySet().toArray(String[]::new)) {
				try {
					loader.loadClass(name);
				} catch (Throwable t) {
					System.err.printf("[VInject] Exception thrown while force loading class %s%n", name);
					t.printStackTrace();
					errored = true;
				}
			}
			if (errored) {
				System.err.println("[VInject] Exceptions encountered while force loading classes, exiting");
				System.exit(0);
			}
		}
		PrintStream err = System.err;
		Thread.currentThread().setContextClassLoader(loader);
		try {
			MethodHandles.publicLookup()
				.findStatic(
					loader.loadClass(ResourceUtil.loadManifest(loader).getMainAttributes().getValue("Main-Class")),
					"main", MethodType.methodType(void.class, String[].class))
				.invokeExact(args);
		} catch (Throwable t) {
			t.printStackTrace(err);
		}
	}
	public void loadInjector(Plugin plugin, Boolean moduleRequired, ClassNode injector) {
		if (!InjectUtil.isRequired(injector, moduleRequired).booleanValue())
			unrequired.add(injector);
		InjectUtil.getClasses(injector,
			path -> getList(injectorsByClassName, ResourceUtil.asValidName(path)).add(injector));
	}
	public void hookJPL(Class<?> jpl) throws Throwable {
		MethodHandles.privateLookupIn(jpl, MethodHandles.lookup())
			.findStaticSetter(jpl, "vinject$loaderFunc", BiFunction.class)
			.invokeExact((BiFunction<String, URL, Object>) this::getLoader);
	}
	public ClassLoader getLoader(String id, URL url) {
		for (Plugin plugin : pluginManager.getPlugins())
			if (plugin.id().contentEquals(id))
				return plugin.getClassLoader();
		try {
			return (ClassLoader) classLoaderConstructor.invokeExact(new URL[]{url});
		} catch (Throwable e) {
			throw new InternalError(e);
		}
	}
	public Class<?> loadPluginClassLoader() throws IOException, ClassNotFoundException {
		loader.setTransformer(ClassLoader.getPlatformClassLoader()::loadClass);
		return loader.defineClass(
			applyInjectors(PCLInjector.injectPCL(ResourceUtil.loadNode(loader, PCLInjector.PLUGIN_CLASS_LOADER))));
	}
	public Class<?> loadJavaPluginLoader() throws IOException, ClassNotFoundException {
		return loader.defineClass(
			applyInjectors(JPLInjector.injectJPL(ResourceUtil.loadNode(loader, JPLInjector.JAVA_PLUGIN_LOADER))));
	}
	public ClassNode applyInjectors(ClassNode node) {
		if (DEBUG)
			out.printf("[VInject] Applying injectors to: %s%n", node.name);
		List<ClassNode> injectors = injectorsByClassName.remove(ResourceUtil.asName(node.name));
		if (injectors != null)
			injectors.forEach(injector -> InjectUtil.modifyTemplate(node, injector));
		return node;
	}
	public boolean containsInjector(String name) {
		return injectorsByClassName.containsKey(name);
	}
	public static <K, V> List<V> getList(Map<K, List<V>> map, K key) {
		return map.computeIfAbsent(key, k -> new LinkedList<>());
	}
}