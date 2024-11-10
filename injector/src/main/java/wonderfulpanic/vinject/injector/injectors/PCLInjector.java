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

package wonderfulpanic.vinject.injector.injectors;

import static org.objectweb.asm.Opcodes.ACC_PRIVATE;
import static org.objectweb.asm.Opcodes.ACC_STATIC;
import static org.objectweb.asm.Opcodes.ACC_SYNTHETIC;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Set;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import wonderfulpanic.vinject.injector.InternalClassLoader;
import wonderfulpanic.vinject.injector.util.InjectUtil;
import wonderfulpanic.vinject.injector.util.ResourceUtil;

public abstract class PCLInjector {
	public static final String PLUGIN_CLASS_LOADER = "com/velocitypowered/proxy/plugin/PluginClassLoader";
	private static final String INTERNAL = "wonderfulpanic/vinject/injector/InternalClassLoader";
	public static ClassNode injectPCL(ClassNode target) throws ClassNotFoundException {
		target.visitField(ACC_PRIVATE | ACC_STATIC | ACC_SYNTHETIC,
			"vinject$velocity", 'L' + INTERNAL + ';', null, null);
		target.interfaces.add(INTERNAL);
		target.methods.remove(InjectUtil.findMethod(target, "<init>", "([Ljava/net/URL;)V"));
		target.methods.remove(InjectUtil.findMethod(target, "loadClass", "(Ljava/lang/String;Z)Ljava/lang/Class;"));
		target.methods.remove(InjectUtil.findMethod(target, "loadClass0", "(Ljava/lang/String;ZZ)Ljava/lang/Class;"));
		ClassNode injector = ResourceUtil.loadNode(PCLInjector.class.getClassLoader(),
			"wonderfulpanic/vinject/injector/injectors/PCLInjector$Injector");
		for (MethodNode method : injector.methods)
			InjectUtil.setMethodOwner(method, injector.name, target.name, name -> "vinject$" + name);
		target.methods.addAll(injector.methods);
		return target;
	}
	@SuppressWarnings("unused")
	private static class Injector extends URLClassLoader implements InternalClassLoader {
		private static Set<Injector> loaders;
		private static InternalClassLoader vinject$velocity;
		public Injector(URL[] urls) {
			super(urls, ClassLoader.getPlatformClassLoader());
		}
		@Override
		public URL getResource(String name) {
			URL url = super.getResource(name);
			if (url != null)
				return url;
			url = vinject$velocity.vinject$getResource(name);
			if (url != null)
				return url;
			for (Injector loader : loaders) {
				if (loader == this)
					continue;
				url = loader.vinject$getResource(name);
				if (url != null)
					return url;
			}
			return null;
		}
		@Override
		protected Class<?> loadClass(String name, boolean ignore) throws ClassNotFoundException {
			try {
				return super.loadClass(name, false);
			} catch (ClassNotFoundException ignored) {

			}
			Class<?> cl = vinject$velocity.vinject$loadClass(name);
			if (cl != null)
				return cl;
			for (Injector loader : loaders) {
				if (loader == this)
					continue;
				try {
					return loader.vinject$loadClass(name);
				} catch (ClassNotFoundException ignored) {

				}
			}
			throw new ClassNotFoundException(name);
		}
		@Override
		public void vinject$addToClassloaders() {
			loaders.add(this);
		}
		@Override
		public URL vinject$getResource(String name) {
			return super.getResource(name);
		}
		@Override
		public Class<?> vinject$loadClass(String name) throws ClassNotFoundException {
			return super.loadClass(name, false);
		}
		@Override
		public Class<?> vinject$defineClass(byte[] bytes) {
			return super.defineClass(null, bytes, 0, bytes.length);
		}
	}
}