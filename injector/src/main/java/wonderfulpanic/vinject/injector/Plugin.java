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
import java.util.LinkedList;
import java.util.List;
import org.objectweb.asm.tree.ClassNode;

public class Plugin {
	private final String id;
	private final File file;
	private final boolean isVInject;
	private List<ClassNode> injectors = new LinkedList<>();
	private ClassLoader loader;
	public Plugin(String id, File file, boolean isVInject) {
		this.id = id;
		this.file = file;
		this.isVInject = isVInject;
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
	public boolean isVInjectPlugin() {
		return isVInject;
	}
	public List<ClassNode> getInjectors() {
		return injectors;
	}
	public Plugin setClassLoader(ClassLoader loader) throws NoSuchMethodException, IllegalAccessException {
		this.loader = loader;
		return this;
	}
	public ClassLoader getClassLoader() {
		return loader;
	}
}