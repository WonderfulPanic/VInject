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

import static wonderfulpanic.vinject.injector.VInjectLoader.DEBUG;
import static wonderfulpanic.vinject.injector.VInjectLoader.EXPORT;
import static wonderfulpanic.vinject.injector.VInjectLoader.out;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import org.objectweb.asm.tree.ClassNode;
import wonderfulpanic.vinject.injector.util.ResourceUtil;

public class VInjectClassLoader extends URLClassLoader implements InternalClassLoader {
	private final VInjectLoader loader;
	private boolean classLoading;
	public VInjectClassLoader(VInjectLoader loader, URL[] urls) throws MalformedURLException {
		super(urls, ClassLoader.getPlatformClassLoader());
		this.loader = loader;
	}
	@Override
	public URL getResource(String name) {
		URL url = super.findResource(name);
		if (url != null)
			return url;
		for (Plugin plugin : loader.getPluginManager().getPlugins()) {
			url = ((InternalClassLoader) plugin.getClassLoader()).vinject$getResource(name);
			if (url != null)
				return url;
		}
		return null;
	}
	@Override
	public Class<?> loadClass(String name, boolean ignore) throws ClassNotFoundException {
		synchronized (getClassLoadingLock(name)) {
			Class<?> cl = findLoadedClass(name);
			if (cl != null)
				return cl;
			String path = ResourceUtil.addClassExt(ResourceUtil.asPath(name));
			if (ClassLoader.getPlatformClassLoader().getResource(path) != null)
				return ClassLoader.getPlatformClassLoader().loadClass(name);
			if (!classLoading) {
				if (name.contentEquals(InternalClassLoader.class.getName()))
					return InternalClassLoader.class;
				throw new ClassNotFoundException(name);
			}
			URL url = super.getResource(path);
			if (url != null) {
				if (!loader.containsInjector(name))
					return super.loadClass(name, false);
				return defineClass(loader.applyInjectors(ResourceUtil.getNode(url)));
			}
			for (Plugin plugin : loader.getPluginManager().getPlugins()) {
				InternalClassLoader accessor = (InternalClassLoader) plugin.getClassLoader();
				url = accessor.vinject$getResource(path);
				if (url == null)
					continue;
				if (DEBUG)
					out.printf("[VInject] Found class %s for velocity in plugin: %s%n", name, plugin.id());
				if (!loader.containsInjector(name))
					return accessor.vinject$loadClass(name);
				return plugin.defineClass(loader.applyInjectors(ResourceUtil.getNode(url)));
			}
			throw new ClassNotFoundException(name);
		}
	}
	@Override
	public Class<?> vinject$loadClass(String name) throws ClassNotFoundException {
		synchronized (getClassLoadingLock(name)) {
			Class<?> cl = findLoadedClass(name);
			if (cl != null)
				return cl;
			String path = ResourceUtil.addClassExt(ResourceUtil.asPath(name));
			if (ClassLoader.getPlatformClassLoader().getResource(path) != null)
				return ClassLoader.getPlatformClassLoader().loadClass(name);
			if (DEBUG)
				out.printf("[VInject] Searching class %s for plugin%n", name);
			URL url = super.getResource(path);
			if (url == null)
				return null;
			if (!loader.containsInjector(name))
				return super.loadClass(name, false);
			return defineClass(loader.applyInjectors(ResourceUtil.getNode(url)));
		}
	}
	@Override
	public URL vinject$getResource(String name) {
		return super.getResource(name);
	}
	@Override
	public void vinject$addToClassloaders() {
		throw new UnsupportedOperationException();
	}
	@Override
	public Class<?> vinject$defineClass(byte[] bytes) {
		throw new UnsupportedOperationException();
	}
	public void enableClassLoading() {
		classLoading = true;
	}
	public void disableClassLoading() {
		classLoading = false;
	}
	public Class<?> defineClass(ClassNode node) {
		if (DEBUG)
			out.printf("[VInject] Class %s defined in velocity%n", node.name);
		byte[] bytes = ResourceUtil.getBytes(node);
		if (EXPORT)
			ResourceUtil.exportClass(bytes, "velocity", node.name);
		return defineClass(null, bytes, 0, bytes.length);
	}
	static {
		ClassLoader.registerAsParallelCapable();
	}
}