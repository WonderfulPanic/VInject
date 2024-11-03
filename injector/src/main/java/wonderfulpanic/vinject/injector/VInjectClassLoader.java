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
import static wonderfulpanic.vinject.injector.VInjectLoader.out;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import org.objectweb.asm.tree.ClassNode;
import wonderfulpanic.vinject.injector.util.ResourceUtil;

public class VInjectClassLoader extends URLClassLoader {
	private static final boolean EXPORT = Boolean.getBoolean("vinject.export");
	private Transformer transformer;
	public VInjectClassLoader(URL[] urls) throws MalformedURLException {
		super(urls, ClassLoader.getPlatformClassLoader());
	}
	@Override
	public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
		synchronized (getClassLoadingLock(name)) {
			Class<?> cl = findLoadedClass(name);
			if (cl != null)
				return cl;
			cl = transformer.load(name);
			if (cl != null)
				return cl;
			return super.loadClass(name, resolve);
		}
	}
	protected Class<?> loadClassForPlugin(VInjectLoader loader, String name) {
		synchronized (getClassLoadingLock(name)) {
			Class<?> cl = findLoadedClass(name);
			if (cl != null)
				return cl;
			try {
				String path = ResourceUtil.asPath(name) + ".class";
				if (getParent().getResource(path) != null)
					return super.loadClass(name, false);
				if (DEBUG)
					out.printf("[VInject] Searching class %s for plugin%n", name);
				try (InputStream in = getResourceAsStream(path)) {
					if (in == null)
						return super.loadClass(name, false);
					if (loader.containsInjector(name))
						return defineClass(loader.applyInjectors(ResourceUtil.getNode(in)));
					else
						return super.loadClass(name, false);
				} catch (IOException e) {
					e.printStackTrace();
				}
				return super.loadClass(name, false);
			} catch (ClassNotFoundException e) {
				return null;
			}
		}
	}
	public void setTransformer(Transformer t0) {
		if (t0 == null)
			throw new NullPointerException("transformer");
		transformer = t0;
	}
	public Class<?> defineClass(ClassNode node) {
		if (DEBUG)
			out.printf("[VInject] Class %s defined in velocity%n", node.name);
		byte[] bytes = ResourceUtil.getBytes(node);
		if (EXPORT) {
			File file = new File("vinject-export/velocity", node.name + ".class");
			try {
				file.getParentFile().mkdirs();
				try (FileOutputStream out = new FileOutputStream(file)) {
					out.write(bytes);
				}
			} catch (IOException e) {
				throw new InternalError(e);
			}
		}
		return defineClass(null, bytes, 0, bytes.length);
	}
	static {
		ClassLoader.registerAsParallelCapable();
	}
	@FunctionalInterface
	public static interface Transformer {
		public Class<?> load(String name) throws ClassNotFoundException;
	}
}