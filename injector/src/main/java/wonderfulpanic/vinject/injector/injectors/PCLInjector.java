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
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACC_STATIC;
import static org.objectweb.asm.Opcodes.ACC_SYNTHETIC;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.ARETURN;
import static org.objectweb.asm.Opcodes.ASTORE;
import static org.objectweb.asm.Opcodes.ATHROW;
import static org.objectweb.asm.Opcodes.CHECKCAST;
import static org.objectweb.asm.Opcodes.DUP;
import static org.objectweb.asm.Opcodes.F_NEW;
import static org.objectweb.asm.Opcodes.GETSTATIC;
import static org.objectweb.asm.Opcodes.GOTO;
import static org.objectweb.asm.Opcodes.IFNE;
import static org.objectweb.asm.Opcodes.IFNULL;
import static org.objectweb.asm.Opcodes.IF_ACMPNE;
import static org.objectweb.asm.Opcodes.ILOAD;
import static org.objectweb.asm.Opcodes.INTEGER;
import static org.objectweb.asm.Opcodes.INVOKEINTERFACE;
import static org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;
import static org.objectweb.asm.Opcodes.NEW;
import static org.objectweb.asm.Opcodes.RETURN;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import wonderfulpanic.vinject.injector.util.InjectUtil;

public abstract class PCLInjector {
	public static final String PLUGIN_CLASS_LOADER = "com/velocitypowered/proxy/plugin/PluginClassLoader";
	private static final String FUNCTION = "java/util/function/Function";
	public static ClassNode injectPCL(ClassNode node) {
		node.methods.remove(InjectUtil.findMethod(node, "<init>", "([Ljava/net/URL;)V"));
		{
			MethodNode method = new MethodNode(ACC_PUBLIC | ACC_SYNTHETIC, "<init>", "([Ljava/net/URL;)V", null,
				new String[]{"java/net/MalformedURLException"});
			method.visitVarInsn(ALOAD, 0);
			method.visitVarInsn(ALOAD, 1);
			method.visitMethodInsn(INVOKESTATIC, "java/lang/ClassLoader", "getPlatformClassLoader",
				"()Ljava/lang/ClassLoader;", false);
			method.visitMethodInsn(INVOKESPECIAL, "java/net/URLClassLoader", "<init>",
				"([Ljava/net/URL;Ljava/lang/ClassLoader;)V", false);
			method.visitInsn(RETURN);
			node.methods.add(method);
		}
		node.methods.remove(InjectUtil.findMethod(node, "loadClass", "(Ljava/lang/String;Z)Ljava/lang/Class;"));
		node.methods.remove(InjectUtil.findMethod(node, "loadClass0", "(Ljava/lang/String;ZZ)Ljava/lang/Class;"));
		node.visitField(ACC_PRIVATE | ACC_STATIC | ACC_SYNTHETIC, "vinject$velocity", 'L' + FUNCTION + ';', null, null);
		{
			MethodNode method = new MethodNode(ACC_PUBLIC | ACC_SYNTHETIC, "loadClass",
				"(Ljava/lang/String;Z)Ljava/lang/Class;", null, new String[]{"java/lang/ClassNotFoundException"});
			Label label0 = new Label(), label1 = new Label(), label2 = new Label();
			method.visitTryCatchBlock(label0, label1, label2, "java/lang/ClassNotFoundException");
			method.visitLabel(label0);
			method.visitVarInsn(ALOAD, 0);
			method.visitVarInsn(ALOAD, 1);
			method.visitVarInsn(ILOAD, 2);
			method.visitMethodInsn(INVOKESPECIAL, "java/net/URLClassLoader", "loadClass",
				"(Ljava/lang/String;Z)Ljava/lang/Class;", false);
			method.visitLabel(label1);
			method.visitInsn(ARETURN);
			method.visitLabel(label2);
			method.visitFrame(F_NEW, 3, new Object[]{PLUGIN_CLASS_LOADER, "java/lang/String", INTEGER}, 1,
				new Object[]{"java/lang/ClassNotFoundException"});
			method.visitVarInsn(ASTORE, 3);
			method.visitFieldInsn(GETSTATIC, PLUGIN_CLASS_LOADER, "vinject$velocity", 'L' + FUNCTION + ';');
			method.visitVarInsn(ALOAD, 1);
			method.visitMethodInsn(INVOKEINTERFACE, FUNCTION, "apply", "(Ljava/lang/Object;)Ljava/lang/Object;", true);
			method.visitTypeInsn(CHECKCAST, "java/lang/Class");
			method.visitVarInsn(ASTORE, 3);
			method.visitVarInsn(ALOAD, 3);
			Label label8 = new Label();
			method.visitJumpInsn(IFNULL, label8);
			method.visitVarInsn(ALOAD, 3);
			method.visitInsn(ARETURN);
			method.visitLabel(label8);
			method.visitFrame(F_NEW, 4,
				new Object[]{PLUGIN_CLASS_LOADER, "java/lang/String", INTEGER, "java/lang/Class"}, 0, new Object[]{});
			method.visitFieldInsn(GETSTATIC, PLUGIN_CLASS_LOADER, "loaders", "Ljava/util/Set;");
			method.visitMethodInsn(INVOKEINTERFACE, "java/util/Set", "iterator", "()Ljava/util/Iterator;", true);
			method.visitVarInsn(ASTORE, 4);
			Label label11 = new Label();
			method.visitJumpInsn(GOTO, label11);
			Label label12 = new Label();
			method.visitLabel(label12);
			method.visitFrame(F_NEW, 5,
				new Object[]{PLUGIN_CLASS_LOADER, "java/lang/String", INTEGER, "java/lang/Class", "java/util/Iterator"},
				0, new Object[]{});
			method.visitVarInsn(ALOAD, 4);
			method.visitMethodInsn(INVOKEINTERFACE, "java/util/Iterator", "next", "()Ljava/lang/Object;", true);
			method.visitTypeInsn(CHECKCAST, PLUGIN_CLASS_LOADER);
			method.visitVarInsn(ASTORE, 5);
			method.visitVarInsn(ALOAD, 5);
			method.visitVarInsn(ALOAD, 0);
			Label label3 = new Label(), label4 = new Label(), label5 = new Label();
			method.visitTryCatchBlock(label3, label4, label5, "java/lang/ClassNotFoundException");
			method.visitJumpInsn(IF_ACMPNE, label3);
			method.visitJumpInsn(GOTO, label11);
			method.visitLabel(label3);
			method.visitLineNumber(51, label3);
			method.visitFrame(F_NEW, 6, new Object[]{PLUGIN_CLASS_LOADER, "java/lang/String", Opcodes.INTEGER,
				"java/lang/Class", "java/util/Iterator", PLUGIN_CLASS_LOADER}, 0, new Object[]{});
			method.visitVarInsn(ALOAD, 5);
			method.visitVarInsn(ALOAD, 1);
			method.visitVarInsn(ILOAD, 2);
			method.visitMethodInsn(INVOKEVIRTUAL, PLUGIN_CLASS_LOADER, "vinject$load",
				"(Ljava/lang/String;Z)Ljava/lang/Class;", false);
			method.visitLabel(label4);
			method.visitInsn(ARETURN);
			method.visitLabel(label5);
			method.visitLineNumber(52, label5);
			method.visitFrame(F_NEW, 6,
				new Object[]{PLUGIN_CLASS_LOADER, "java/lang/String", Opcodes.INTEGER, "java/lang/Class",
					"java/util/Iterator", PLUGIN_CLASS_LOADER},
				1, new Object[]{"java/lang/ClassNotFoundException"});
			method.visitVarInsn(ASTORE, 6);
			method.visitLabel(label11);
			method.visitLineNumber(46, label11);
			method.visitFrame(Opcodes.F_NEW, 5, new Object[]{PLUGIN_CLASS_LOADER, "java/lang/String", Opcodes.INTEGER,
				"java/lang/Class", "java/util/Iterator"}, 0, new Object[]{});
			method.visitVarInsn(ALOAD, 4);
			method.visitMethodInsn(INVOKEINTERFACE, "java/util/Iterator", "hasNext", "()Z", true);
			method.visitJumpInsn(IFNE, label12);
			method.visitTypeInsn(NEW, "java/lang/ClassNotFoundException");
			method.visitInsn(DUP);
			method.visitVarInsn(ALOAD, 1);
			method.visitMethodInsn(INVOKESPECIAL, "java/lang/ClassNotFoundException",
				"<init>", "(Ljava/lang/String;)V", false);
			method.visitInsn(ATHROW);
			node.methods.add(method);
		}
		{
			MethodNode method = new MethodNode(ACC_PUBLIC | ACC_SYNTHETIC, "vinject$load",
				"(Ljava/lang/String;Z)Ljava/lang/Class;", null, new String[]{"java/lang/ClassNotFoundException"});
			method.visitVarInsn(ALOAD, 0);
			method.visitVarInsn(ALOAD, 1);
			method.visitVarInsn(ILOAD, 2);
			method.visitMethodInsn(INVOKESPECIAL, "java/net/URLClassLoader", "loadClass",
				"(Ljava/lang/String;Z)Ljava/lang/Class;", false);
			method.visitInsn(ARETURN);
			node.methods.add(method);
		}
		return node;
	}
}