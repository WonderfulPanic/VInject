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

import static org.objectweb.asm.Opcodes.ACC_STATIC;
import static org.objectweb.asm.Opcodes.ACC_SYNTHETIC;
import static org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.IRETURN;
import static org.objectweb.asm.Opcodes.RETURN;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Pattern;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;

public abstract class InjectUtil {
	private static final Pattern lambdaPattern = Pattern.compile("^lambda\\$.*\\d+$");
	private static final String BASE = "Lwonderfulpanic/vinject/api/";
	private static final String INJECTOR = BASE + "Injector;";
	private static final String SHADOW = BASE + "Shadow;";
	private static final String OVERWRITE = BASE + "Overwrite;";
	public static void getClasses(ClassNode node, Consumer<String> consumer) {
		List<Object> attr = getAnnotation(node.invisibleAnnotations, INJECTOR).values;
		for (int i = 0; i < attr.size(); i += 2)
			switch ((String) attr.get(i)) {
				case "value" -> ((List<?>) attr.get(i | 1)).forEach(v -> consumer.accept(((Type) v).getInternalName()));
				case "classPaths" -> ((List<?>) attr.get(i | 1)).forEach(v -> consumer.accept((String) v));
			}
	}
	public static Boolean isRequired(ClassNode node, Boolean def) {
		List<Object> attr = getAnnotation(node.invisibleAnnotations, INJECTOR).values;
		for (int i = 0; i < attr.size(); i += 2)
			if (((String) attr.get(i)).contentEquals("required"))
				return (Boolean) attr.get(i | 1);
		return def;
	}
	/*public static void forEachAttr(List<Object>attr,BiConsumer<String,Object>consumer){
		for(int i=0;i<attr.size();i+=2)
			consumer.accept((String)attr.get(i),attr.get(i|1));
	}*/
	public static ClassNode modifyTemplate(ClassNode target, ClassNode template) {
		target.interfaces.addAll(template.interfaces);
		for (FieldNode field : template.fields) {
			if (hasAnnotation(field.invisibleAnnotations, SHADOW)) {
				FieldNode targetField = findField(target, field.name);
				validate(targetField == null, target, field, "not found");
				validate(((targetField.access ^ field.access) & ACC_STATIC) != 0, target, field,
					"has different static access");
				validate(!targetField.desc.contentEquals(field.desc), target, field, "has different type");
			} else {
				validate(findField(target, field.name) != null, target, field, "already exists");
				target.fields.add(field);
			}
		}
		String simpleName = template.name.substring(template.name.lastIndexOf('/') + 1, template.name.length());
		for (MethodNode method : template.methods) {
			MethodNode targetMethod = findMethod(target, method.name, method.desc.replace(template.name, target.name));
			if (hasAnnotation(method.invisibleAnnotations, SHADOW)) {
				validate(targetMethod == null, target, method, "Method not found");
				continue;
			}
			setMethodOwner(method, template.name, target.name, name -> simpleName + "$" + name);
			if (hasAnnotation(method.invisibleAnnotations, OVERWRITE)) {
				validate(targetMethod == null, target, method, "Method not found");
				targetMethod.instructions = method.instructions;
			} else if (method.name.contentEquals("<init>")) {
				validate(targetMethod == null && !method.desc.contentEquals("()V"), target,
					"Only empty or similar constructors are allowed", method.desc);
				removeSuperConstructor(method.instructions);
				forEachConstructor(target, m2 -> appendToConstructor(m2.instructions, method.instructions));
			} else if (method.name.contentEquals("<clinit>")) {
				if (targetMethod == null)
					target.methods.add(method);
				else
					appendToConstructor(targetMethod.instructions, method.instructions);
			} else if ((method.access & ACC_SYNTHETIC) == ACC_SYNTHETIC &&
				lambdaPattern.matcher(method.name).matches()) {
				validate(targetMethod != null, target, method, "Lambda method already exists");
				method.name = simpleName + "$" + method.name;
				if (method.localVariables != null)
					method.localVariables.forEach(var -> {
						if (var.desc.regionMatches(1, template.name, 0, template.name.length()))
							var.desc = 'L' + target.name + ';';
					});
				target.methods.add(method);
			} else {
				validate(targetMethod != null, target, method, "Method already exists");
				target.methods.add(method);
			}
		}
		return target;
	}
	public static MethodNode setMethodOwner(MethodNode target, String from, String to,
		Function<String, String> lambdaRenamer) {
		for (AbstractInsnNode insn : target.instructions) {
			if (insn instanceof TypeInsnNode type) {
				if (type.desc.contentEquals(from))
					type.desc = to;
			} else if (insn instanceof FieldInsnNode field) {
				if (field.owner.contentEquals(from))
					field.owner = to;
			} else if (insn instanceof MethodInsnNode method) {
				if (method.owner.contentEquals(from))
					method.owner = to;
			} else if (insn instanceof InvokeDynamicInsnNode invoke) {
				if (invoke.bsm.getOwner().contentEquals("java/lang/invoke/LambdaMetafactory") &&
					invoke.bsm.getName().contentEquals("metafactory") &&
					invoke.bsmArgs[1] instanceof Handle handle &&
					lambdaPattern.matcher(handle.getName()).matches()) {
					invoke.desc = invoke.desc.replace(from, to);
					invoke.bsmArgs[1] = new Handle(handle.getTag(), to, lambdaRenamer.apply(handle.getName()),
						handle.getDesc(), handle.isInterface());
				}
			} else if (insn instanceof FrameNode frame) {
				if (frame.local != null)
					frame.local.replaceAll(obj -> from.equals(obj) ? to : obj);
				if (frame.stack != null)
					frame.stack.replaceAll(obj -> from.equals(obj) ? to : obj);
			}
		}
		return target;
	}
	public static void validate(boolean expression, ClassNode node, Object subject, String desc) {
		if (!expression)
			return;
		String err = "Error modifying " + node.name + ": " + desc;
		if (subject instanceof MethodNode method)
			throw new IllegalStateException(err + " (" + method.name + "/" + method.desc + ")");
		else if (subject instanceof FieldNode field)
			throw new IllegalStateException("Target field " + err + " (" + field.name + "/" + field.desc + ")");
		else
			throw new IllegalArgumentException("Unknown subject: " + subject);
	}
	public static void removeSuperConstructor(InsnList list) {
		for (AbstractInsnNode insn = list.getFirst();;) {
			AbstractInsnNode r = insn;
			insn = insn.getNext();
			list.remove(r);
			if (r instanceof MethodInsnNode node) {
				if (node.getOpcode() != INVOKESPECIAL || !node.name.contentEquals("<init>"))
					throw new IllegalStateException("Super constructor not found");
				break;
			}
		}
	}
	public static InsnList appendToConstructor(InsnList insn, InsnList append) {
		if (removeReturn(insn) != RETURN)
			throw new IllegalArgumentException("Constructor method should not return value");
		insn.add(cloneInsnList(append));
		return insn;
	}
	public static int removeReturn(InsnList list) {
		for (AbstractInsnNode insn = list.getLast();;) {
			AbstractInsnNode remove = insn;
			insn = insn.getPrevious();
			list.remove(remove);
			if (remove instanceof InsnNode node) {
				int opcode = node.getOpcode();
				if (opcode < IRETURN || opcode > RETURN)
					throw new IllegalArgumentException("Expected any return at the end, got " + opcode);
				return opcode;
			}
		}
	}
	/*public static void removeLineNumbers(InsnList list){
		for(AbstractInsnNode insn=list.getLast();insn!=null;){
			AbstractInsnNode r=insn;
			insn=r.getPrevious();
			if(r instanceof LineNumberNode){
				list.remove(r);
			}
		}
	}*/
	public static InsnList cloneInsnList(InsnList insnList) {
		MethodNode node = new MethodNode();
		insnList.accept(node);
		return node.instructions;
	}
	public static FieldNode findField(ClassNode node, String name) {
		for (FieldNode field : node.fields)
			if (name.contentEquals(field.name))
				return field;
		return null;
	}
	public static MethodNode findMethod(ClassNode node, String name, String desc) {
		for (MethodNode method : node.methods)
			if (name.contentEquals(method.name) && desc.contentEquals(method.desc))
				return method;
		return null;
	}
	public static boolean hasAnnotation(List<AnnotationNode> list, String desc) {
		return getAnnotation(list, desc) != null;
	}
	public static AnnotationNode getAnnotation(List<AnnotationNode> list, String desc) {
		if (list == null)
			return null;
		for (AnnotationNode node : list)
			if (desc.contentEquals(node.desc))
				return node;
		return null;
	}
	public static void forEachConstructor(ClassNode node, Consumer<MethodNode> consumer) {
		for (MethodNode method : node.methods)
			if (method.name.contentEquals("<init>"))
				consumer.accept(method);
	}
}
