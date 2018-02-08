/*
 * ShiftLeft, Inc. CONFIDENTIAL
 * Unpublished Copyright (c) 2017 ShiftLeft, Inc., All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains the property of ShiftLeft, Inc.
 * The intellectual and technical concepts contained herein are proprietary to ShiftLeft, Inc.
 * and may be covered by U.S. and Foreign Patents, patents in process, and are protected by
 * trade secret or copyright law. Dissemination of this information or reproduction of this
 * material is strictly forbidden unless prior written permission is obtained
 * from ShiftLeft, Inc. Access to the source code contained herein is hereby forbidden to
 * anyone except current ShiftLeft, Inc. employees, managers or contractors who have executed
 * Confidentiality and Non-disclosure agreements explicitly covering such access.
 *
 * The copyright notice above does not evidence any actual or intended publication or disclosure
 * of this source code, which includeas information that is confidential and/or proprietary, and
 * is a trade secret, of ShiftLeft, Inc.
 *
 * ANY REPRODUCTION, MODIFICATION, DISTRIBUTION, PUBLIC PERFORMANCE, OR PUBLIC DISPLAY
 * OF OR THROUGH USE OF THIS SOURCE CODE WITHOUT THE EXPRESS WRITTEN CONSENT OF ShiftLeft, Inc.
 * IS STRICTLY PROHIBITED, AND IN VIOLATION OF APPLICABLE LAWS AND INTERNATIONAL TREATIES.
 * THE RECEIPT OR POSSESSION OF THIS SOURCE CODE AND/OR RELATED INFORMATION DOES NOT
 * CONVEY OR IMPLY ANY RIGHTS TO REPRODUCE, DISCLOSE OR DISTRIBUTE ITS
 * CONTENTS, OR TO MANUFACTURE, USE, OR SELL ANYTHING THAT IT MAY DESCRIBE, IN WHOLE OR IN PART.
 */
package io.shiftleft.bctrace.asm.helper;

import io.shiftleft.bctrace.asm.util.ASMUtils;
import java.util.List;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * Creates a wrapper implementation for a native method. <br><br> For example, this helper turns the
 * method node instructions of a method like this:
 * <br><pre>{@code
 * native boolean foo(int x);
 * }
 * </pre>
 * Into that:
 * <br><pre>{@code
 * boolean foo(int x) {
 *   return ${prefix}foo(x);
 * }
 * }
 * </pre>
 *
 * @author Ignacio del Valle Alles idelvall@shiftleft.io
 * @see <a href="https://docs.oracle.com/javase/8/docs/api/java/lang/instrument/Instrumentation.html#setNativeMethodPrefix-java.lang.instrument.ClassFileTransformer-java.lang.String-">Instrumentation.setNativeMethodPrefix()</a>
 */
public class NativeWrapperHelper extends Helper {

  public static void createWrapperImpl(ClassNode cn, MethodNode mn, String prefix,
      List<MethodNode> newMethods) {
    cloneAndRenameNativeMethod(cn, mn, prefix, newMethods);
    InsnList il = new InsnList();
    mn.instructions = il;
    mn.access = mn.access & ~Opcodes.ACC_NATIVE;
    pushArguments(il, mn);
    if (ASMUtils.isStatic(mn.access)) {
      il.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
          cn.name, prefix + mn.name,
          mn.desc, ASMUtils.isInterface(mn.access)));
    } else {
      il.add(new MethodInsnNode(Opcodes.INVOKESPECIAL,
          cn.name, prefix + mn.name,
          mn.desc, ASMUtils.isInterface(mn.access)));
    }
    Type returnType = Type.getReturnType(mn.desc);
    if (returnType.getDescriptor().equals("V")) {
      il.add(new InsnNode(Opcodes.RETURN));
    } else {
      il.add(ASMUtils.getReturnInst(returnType));
    }
  }

  private static void cloneAndRenameNativeMethod(ClassNode cn, MethodNode mn, String prefix,
      List<MethodNode> newMethods) {
    MethodNode cloned = new MethodNode();
    cloned.name = prefix + mn.name;

    cloned.access = mn.access;
    cloned.instructions = mn.instructions;
    cloned.desc = mn.desc;
    cloned.signature = mn.signature;
    cloned.annotationDefault = mn.annotationDefault;
    cloned.attrs = mn.attrs;
    cloned.exceptions = mn.exceptions;
    cloned.invisibleAnnotations = mn.invisibleAnnotations;
    cloned.invisibleParameterAnnotations = mn.invisibleParameterAnnotations;
    cloned.invisibleLocalVariableAnnotations = mn.invisibleLocalVariableAnnotations;
    cloned.invisibleTypeAnnotations = mn.invisibleTypeAnnotations;
    cloned.localVariables = mn.localVariables;
    cloned.maxLocals = mn.maxLocals;
    cloned.maxStack = mn.maxStack;
    cloned.parameters = mn.parameters;
    cloned.tryCatchBlocks = mn.tryCatchBlocks;
    cloned.visibleAnnotations = mn.visibleAnnotations;
    cloned.visibleLocalVariableAnnotations = mn.visibleLocalVariableAnnotations;
    cloned.visibleParameterAnnotations = mn.visibleParameterAnnotations;
    cloned.visibleTypeAnnotations = mn.visibleTypeAnnotations;

    newMethods.add(cloned);
  }

  private static void pushArguments(InsnList il, MethodNode mn) {
    Type[] methodArguments = Type.getArgumentTypes(mn.desc);
    int index = ASMUtils.isStatic(mn.access) ? 0 : 1;
    for (int i = 0; i < methodArguments.length; i++) {
      il.add(ASMUtils.getLoadInst(methodArguments[i], index));
    }
  }
}
