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
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.CHECKCAST;
import static org.objectweb.asm.Opcodes.GETSTATIC;
import static org.objectweb.asm.Opcodes.INVOKEINTERFACE;
import static org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.NEW;
import java.util.ListIterator;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;
import wonderfulpanic.vinject.injector.util.InjectUtil;

public abstract class JPLInjector {
	public static final String JAVA_PLUGIN_LOADER = "com/velocitypowered/proxy/plugin/loader/java/JavaPluginLoader";
	public static ClassNode injectJPL(ClassNode node) {
		node.visitField(ACC_PRIVATE | ACC_STATIC | ACC_SYNTHETIC, "vinject$loaderFunc", "Ljava/util/function/Function;",
			null, null);
		MethodNode method = InjectUtil.findMethod(node, "createPluginFromCandidate",
			"(Lcom/velocitypowered/api/plugin/PluginDescription;)Lcom/velocitypowered/api/plugin/PluginDescription;");
		method.access |= ACC_SYNTHETIC;
		InsnList instructions = method.instructions;
		ListIterator<AbstractInsnNode> iter = instructions.iterator();
		while (iter.hasNext()) {
			AbstractInsnNode insn = iter.next();
			if (!(insn instanceof TypeInsnNode type) || type.getOpcode() != NEW ||
				!type.desc.contentEquals("com/velocitypowered/proxy/plugin/PluginClassLoader"))
				continue;
			iter.set(new FieldInsnNode(GETSTATIC, node.name, "vinject$loaderFunc", "Ljava/util/function/Function;"));
			while (iter.hasNext()) {
				insn = iter.next();
				if (insn instanceof VarInsnNode var && var.getOpcode() == ALOAD)
					continue;
				if (!(insn instanceof MethodInsnNode m) || m.getOpcode() != INVOKESPECIAL ||
					!m.owner.contentEquals("com/velocitypowered/proxy/plugin/PluginClassLoader")) {
					iter.remove();
					continue;
				}
				iter.set(new MethodInsnNode(INVOKEINTERFACE, "java/util/function/Function", "apply",
					"(Ljava/lang/Object;)Ljava/lang/Object;", true));
				iter.add(new TypeInsnNode(CHECKCAST, "com/velocitypowered/proxy/plugin/PluginClassLoader"));
				return node;
			}
		}
		throw new InternalError("Could not hook up JavaPluginLoader");
	}
}