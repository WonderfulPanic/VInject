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

import java.net.URL;

public interface InternalClassLoader {
	public URL vinject$getResource(String name);
	public Class<?> vinject$loadClass(String name, String plugin) throws ClassNotFoundException;
	//VInjectClassLoader only
	public default byte[] vinject$injectClass(String name, URL url, String plugin) throws ClassNotFoundException {
		throw new UnsupportedOperationException();
	}
}