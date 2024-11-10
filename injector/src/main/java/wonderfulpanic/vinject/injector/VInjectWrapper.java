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

import static wonderfulpanic.vinject.injector.VInjectLoader.VERSION;
import java.io.File;
import java.io.FileNotFoundException;
import java.net.URL;
import java.util.Arrays;

public abstract class VInjectWrapper {
	public static void main(String[] args) throws Throwable {
		System.out.println("[VInject] VInject version " + VERSION);
		if (args.length == 0)
			throw new IllegalArgumentException("VInject requires to pass velocity's jar name as first argument. " +
				"Installation instructions can be found here: https://github.com/WonderfulPanic/VInject");
		File velocity = new File(args[0]);
		if (!velocity.exists())
			throw new FileNotFoundException(
				String.format("File %s (%s) not found", args[0], velocity.getAbsolutePath()));
		new VInjectLoader(new URL[]{velocity.toURI().toURL()}).load(Arrays.copyOfRange(args, 1, args.length));
	}
}