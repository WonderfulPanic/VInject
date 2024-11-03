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

package wonderfulpanic.vinject.injector.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public abstract class ResourceUtil {
	public static JsonObject getJson(ZipFile zip, String path) throws IOException {
		ZipEntry entry = zip.getEntry(path);
		if (entry == null)
			return null;
		try (InputStreamReader reader = new InputStreamReader(zip.getInputStream(entry))) {
			return JsonParser.parseReader(reader).getAsJsonObject();
		}
	}
	public static Manifest loadManifest(ClassLoader loader) throws IOException {
		try (InputStream in = loader.getResourceAsStream("META-INF/MANIFEST.MF")) {
			if (in == null)
				throw new IOException("File META-INF/MANIFEST.MF not found in class path");
			return new Manifest(in);
		}
	}
	public static ClassNode loadNode(ZipFile zip, String path) throws IOException, ClassNotFoundException {
		ClassNode node = getNode(zip, path);
		if (node == null)
			throw new ClassNotFoundException(asValidName(path));
		return node;
	}
	public static ClassNode loadNode(ClassLoader loader, String path) throws IOException, ClassNotFoundException {
		ClassNode node = getNode(loader, path);
		if (node == null)
			throw new ClassNotFoundException(asValidName(path));
		return node;
	}
	public static ClassNode getNode(ZipFile zip, String path) throws IOException {
		ZipEntry entry = zip.getEntry(path.concat(".class"));
		if (entry == null)
			return null;
		try (InputStream in = zip.getInputStream(entry)) {
			return getNode(in);
		}
	}
	public static ClassNode getNode(ClassLoader loader, String path) throws IOException {
		try (InputStream in = loader.getResourceAsStream(path.concat(".class"))) {
			return in == null ? null : getNode(in);
		}
	}
	public static ClassNode getNode(InputStream in) throws IOException {
		ClassNode node = new ClassNode();
		new ClassReader(in).accept(node, 0);
		return node;
	}
	public static byte[] getBytes(ClassNode node) {
		ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
		node.accept(writer);
		return writer.toByteArray();
	}
	public static String asValidPath(String name) {
		return asPath(validateName(name));
	}
	public static String asValidName(String path) {
		return asName(validatePath(path));
	}
	public static String asPath(String name) {
		return name.replace('.', '/');
	}
	public static String asName(String path) {
		return path.replace('/', '.');
	}
	public static String validatePath(String path) {
		if (path.contains("."))
			throw new IllegalArgumentException("Class path should not contain dots: ".concat(path));
		return path;
	}
	public static String validateName(String name) {
		if (name.contains("/"))
			throw new IllegalArgumentException("Class name should not contain slashes: ".concat(name));
		return name;
	}
}