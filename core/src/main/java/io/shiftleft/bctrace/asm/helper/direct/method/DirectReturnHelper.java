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
package io.shiftleft.bctrace.asm.helper.direct.method;

import io.shiftleft.bctrace.asm.CallbackTransformer;
import io.shiftleft.bctrace.asm.helper.Helper;
import io.shiftleft.bctrace.asm.util.ASMUtils;
import io.shiftleft.bctrace.hook.Hook;
import io.shiftleft.bctrace.runtime.listener.direct.DirectListener;
import io.shiftleft.bctrace.runtime.listener.direct.DirectListener.ListenerType;
import java.util.ArrayList;
import java.util.Iterator;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

public class DirectReturnHelper extends Helper {

  public boolean addByteCodeInstructions(ClassNode cn, MethodNode mn,
      ArrayList<Integer> hooksToUse) {

    ArrayList<Integer> listenersToUse = getDirectListenersOfType(hooksToUse, ListenerType.onReturn);
    if (!isInstrumentationNeeded(listenersToUse)) {
      return false;
    }
    addReturnTrace(cn, mn, listenersToUse);
    return true;
  }

  private void addReturnTrace(ClassNode cn, MethodNode mn,
      ArrayList<Integer> listenersToUse) {

    InsnList il = mn.instructions;
    Iterator<AbstractInsnNode> it = il.iterator();

    while (it.hasNext()) {
      AbstractInsnNode abstractInsnNode = it.next();
      switch (abstractInsnNode.getOpcode()) {
        case Opcodes.RETURN:
          il.insertBefore(abstractInsnNode,
              getReturnVoidTraceInstructions(cn, mn, listenersToUse));
          break;
        case Opcodes.IRETURN:
        case Opcodes.LRETURN:
        case Opcodes.FRETURN:
        case Opcodes.ARETURN:
        case Opcodes.DRETURN:
          il.insertBefore(abstractInsnNode,
              getReturnMutatorTraceInstructions(cn, mn, listenersToUse));
      }
    }
  }

  private InsnList getReturnVoidTraceInstructions(ClassNode cn, MethodNode mn,
      ArrayList<Integer> listenersToUse) {

    InsnList il = new InsnList();
    Hook[] hooks = bctrace.getHooks();
    for (int i = listenersToUse.size() - 1; i >= 0; i--) {
      Integer index = listenersToUse.get(i);
      DirectListener listener = (DirectListener) hooks[index].getListener();

      il.add(ASMUtils.getPushInstruction(index)); // hook id
      il.add(getClassConstantReference(Type.getObjectType(cn.name), cn.version)); // caller class
      pushInstance(il, mn); // current instance
      pushMethodArgs(il, mn); // method args
      // Invoke dynamically generated callback method. See CallbackTransformer
      il.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
          "io/shiftleft/bctrace/runtime/Callback",
          CallbackTransformer.getDynamicListenerMethodName(listener),
          CallbackTransformer.getDynamicListenerVoidMethodDescriptor(listener),
          false));
    }
    return il;
  }

  private InsnList getReturnMutatorTraceInstructions(ClassNode cn, MethodNode mn,
      ArrayList<Integer> listenersToUse) {

    Type returnType = Type.getReturnType(mn.desc);
    InsnList il = new InsnList();
    Hook[] hooks = bctrace.getHooks();
    int returnVarIndex = mn.maxLocals;
    mn.maxLocals = mn.maxLocals + returnType.getSize();
    // Store original return value into a local variable
    il.add(ASMUtils.getStoreInst(returnType, returnVarIndex));
    for (int i = listenersToUse.size() - 1; i >= 0; i--) {
      Integer index = listenersToUse.get(i);
      DirectListener listener = (DirectListener) hooks[index].getListener();

      il.add(ASMUtils.getPushInstruction(index)); // hook id
      il.add(ASMUtils.getLoadInst(returnType, returnVarIndex)); // original value consumed from the stack
      il.add(getClassConstantReference(Type.getObjectType(cn.name), cn.version)); // caller class
      pushInstance(il, mn); // current instance
      pushMethodArgs(il, mn); // method args
      il.add(ASMUtils.getLoadInst(returnType, returnVarIndex));
      // Invoke dynamically generated callback method. See CallbackTransformer
      il.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
          "io/shiftleft/bctrace/runtime/Callback",
          CallbackTransformer.getDynamicListenerMethodName(listener),
          CallbackTransformer.getDynamicListenerMutatorMethodDescriptor(listener),
          false));
      // Update return value local variable, so each listener receives the modified value from the ones before
      // instead of getting all of them the original value
      il.add(ASMUtils.getStoreInst(returnType, returnVarIndex));
    }
    il.add(ASMUtils.getLoadInst(returnType, returnVarIndex));
    return il;
  }
}
