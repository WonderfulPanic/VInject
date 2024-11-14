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
			URL url = vinject$getResource(path);
			if (url != null) {
				byte[] bytes = vinject$injectClass(name, url, "velocity");
				if (bytes != null)
					return super.defineClass(null, bytes, 0, bytes.length);
				return super.loadClass(name, false);
			}
			for (Plugin plugin : loader.getPluginManager().getPlugins()) {
				InternalClassLoader internal = (InternalClassLoader) plugin.getClassLoader();
				url = internal.vinject$getResource(path);
				if (url == null)
					continue;
				if (DEBUG)
					out.printf("[VInject] [velocity] Found class %s in %s%n", name, plugin.id());
				return internal.vinject$loadClass(name, null);
			}
			throw new ClassNotFoundException(name);
		}
	}
	@Override
	public Class<?> vinject$loadClass(String name, String plugin) throws ClassNotFoundException {
		synchronized (getClassLoadingLock(name)) {
			Class<?> cl = findLoadedClass(name);
			if (cl != null)
				return cl;
			if (DEBUG)
				out.printf("[VInject] [%s] Searching class %s%n", plugin, name);
			String path = ResourceUtil.addClassExt(ResourceUtil.asPath(name));
			URL url = super.getResource(path);
			if (url == null)
				return null;
			byte[] bytes = vinject$injectClass(name, url, "velocity");
			if (bytes != null)
				return super.defineClass(null, bytes, 0, bytes.length);
			return super.loadClass(name, false);
		}
	}
	@Override
	public URL vinject$getResource(String name) {
		return super.getResource(name);
	}
	@Override
	public byte[] vinject$injectClass(String name, URL url, String plugin) throws ClassNotFoundException {
		if (!loader.containsInjector(name))
			return null;
		if (DEBUG)
			out.printf("[VInject] [%s] Applying injectors to: %s%n", plugin, name);
		return getBytes(loader.applyInjectors(ResourceUtil.getNode(url)), plugin);
	}
	public void enableClassLoading() {
		classLoading = true;
	}
	public void disableClassLoading() {
		classLoading = false;
	}
	protected Class<?> defineClass(ClassNode node) {
		byte[] bytes = getBytes(node, "velocity");
		return super.defineClass(null, bytes, 0, bytes.length);
	}
	private static byte[] getBytes(ClassNode node, String id) {
		if (DEBUG)
			out.printf("[VInject] [%s] Class defined: %s%n", id, ResourceUtil.asName(node.name));
		byte[] bytes = ResourceUtil.getBytes(node);
		if (EXPORT)
			ResourceUtil.exportClass(bytes, id, node.name);
		return bytes;
	}
	static {
		ClassLoader.registerAsParallelCapable();
	}
}