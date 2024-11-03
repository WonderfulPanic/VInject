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
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.LinkedList;
import java.util.List;
import org.objectweb.asm.tree.ClassNode;
import wonderfulpanic.vinject.injector.util.ResourceUtil;

public class Plugin {
	private final File file;
	private final String id;
	private List<ClassNode> injectors = new LinkedList<>();
	private ClassLoader loader;
	private MethodHandle defineFunction;
	public Plugin(String id, File file) {
		this.id = id;
		this.file = file;
	}
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder().append("Plugin [");
		if (id != null)
			builder.append("id=").append(id).append(", ");
		if (file != null)
			builder.append("file=").append(file).append(", ");
		if (injectors != null)
			builder.append("injectors=").append(injectors).append(", ");
		if (loader != null)
			builder.append("loader=").append(loader);
		return builder.append("]").toString();
	}
	public String id() {
		return id;
	}
	public File getPluginFile() {
		return file;
	}
	public List<ClassNode> getInjectors() {
		return injectors;
	}
	public Plugin setClassLoader(ClassLoader loader) throws NoSuchMethodException, IllegalAccessException {
		this.loader = loader;
		if (loader == null)
			defineFunction = null;
		else
			defineFunction = MethodHandles
				.privateLookupIn(loader.getClass(), MethodHandles.lookup()).findVirtual(loader.getClass(),
					"defineClass", MethodType.methodType(Class.class, byte[].class, int.class, int.class))
				.bindTo(loader);
		return this;
	}
	public ClassLoader getClassLoader() {
		return loader;
	}
	public Class<?> defineClass(ClassNode node) throws Throwable {
		if (DEBUG)
			out.printf("[VInject] Class %s defined in %s n", node.name, id);
		byte[] bytes = ResourceUtil.getBytes(node);
		return (Class<?>) defineFunction.invokeExact(bytes, 0, bytes.length);
	}
}