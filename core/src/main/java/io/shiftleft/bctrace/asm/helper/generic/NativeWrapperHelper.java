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
package io.shiftleft.bctrace.asm.helper.generic;

import io.shiftleft.bctrace.asm.helper.Helper;
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

  public void createWrapperImpl(ClassNode cn, MethodNode mn, String prefix,
      List<MethodNode> newMethods) {
    MethodNode clonedMethod = cloneAndRenameNativeMethod(mn, prefix);
    newMethods.add(clonedMethod);
    InsnList il = new InsnList();
    mn.instructions = il;
    mn.access = mn.access & ~Opcodes.ACC_NATIVE;
    pushArguments(il, mn);
    int invokeOpCode;
    if (ASMUtils.isStatic(mn.access)) {
      invokeOpCode = Opcodes.INVOKESTATIC;
    } else {
      invokeOpCode = Opcodes.INVOKESPECIAL;
    }
    il.add(new MethodInsnNode(invokeOpCode, cn.name, prefix + mn.name, mn.desc,
        ASMUtils.isInterface(mn.access)));
    Type returnType = Type.getReturnType(mn.desc);
    if (returnType.getDescriptor().equals("V")) { // if method returns void
      il.add(new InsnNode(Opcodes.RETURN));
    } else {
      il.add(ASMUtils.getReturnInst(returnType));
    }
  }

  private MethodNode cloneAndRenameNativeMethod(MethodNode mn, String prefix) {
    MethodNode cloned = ASMUtils.cloneMethod(mn);
    cloned.name = prefix + mn.name;
    return cloned;
  }

  private void pushArguments(InsnList il, MethodNode mn) {
    Type[] methodArguments = Type.getArgumentTypes(mn.desc);
    int index = ASMUtils.isStatic(mn.access) ? 0 : 1;
    for (int i = 0; i < methodArguments.length; i++) {
      il.add(ASMUtils.getLoadInst(methodArguments[i], index));
    }
  }
}
