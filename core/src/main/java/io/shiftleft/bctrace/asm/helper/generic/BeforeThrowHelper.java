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
import io.shiftleft.bctrace.runtime.listener.generic.BeforeThrownListener;
import io.shiftleft.bctrace.runtime.listener.generic.StartListener;
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

/**
 * Inserts into the method bytecodes, the instructions needed to notify the registered listeners
 * before a throwable is directly thrown by the target method.
 *
 * This helper turns throw instructions like these:
 * <br><pre>{@code
 * throw aException;
 * }
 * </pre>
 * Into that:
 * <br><pre>{@code
 * // Notify the event to the listeners that apply to this method (suppose methodId 1550)
 * Callback.onBeforeThrown(aException, 1550, clazz, this, 0);
 * Callback.onBeforeThrown(aException, 1550, clazz, this, 2);
 * Callback.onBeforeThrown(aException, 1550, clazz, this, 10);
 * ...
 * throw aException;
 * }
 * @author Ignacio del Valle Alles idelvall@shiftleft.io
 */
public class BeforeThrowHelper extends Helper {


  public boolean addByteCodeInstructions(int methodId, ClassNode cn, MethodNode mn,
      ArrayList<Integer> hooksToUse) {
    ArrayList<Integer> listenersToUse = getListenersOfType(hooksToUse,
        BeforeThrownListener.class);

    if (!isInstrumentationNeeded(listenersToUse)) {
      return false;
    }
    InsnList il = mn.instructions;
    Iterator<AbstractInsnNode> it = il.iterator();
    while (it.hasNext()) {
      AbstractInsnNode abstractInsnNode = it.next();

      switch (abstractInsnNode.getOpcode()) {
        case Opcodes.ATHROW:
          il.insertBefore(abstractInsnNode,
              getThrowTraceInstructions(methodId, cn, mn, listenersToUse));
          break;
      }
    }
    return true;
  }

  private InsnList getThrowTraceInstructions(int methodId, ClassNode cn, MethodNode mn,
      ArrayList<Integer> listenersToUse) {
    InsnList il = new InsnList();
    for (int i = 0; i < listenersToUse.size(); i++) {
      Integer index = listenersToUse.get(i);
      BeforeThrownListener listener = (BeforeThrownListener) bctrace.getHooks()[index]
          .getListener();
      if (listener.requiresThrowable()) {
        il.add(new InsnNode(Opcodes.DUP)); // dup throwable
      } else {
        il.add(new InsnNode(Opcodes.ACONST_NULL));
      }
      if (listener.requiresMethodId()) {
        il.add(ASMUtils.getPushInstruction(methodId)); // method id
      } else {
        il.add(ASMUtils.getPushInstruction(0));
      }
      if (listener.requiresClass()) {
        il.add(getClassConstantReference(Type.getObjectType(cn.name), cn.version));
      } else {
        il.add(new InsnNode(Opcodes.ACONST_NULL));
      }
      if (listener.requiresInstance()) {
        pushInstance(il, mn); // current instance
      } else {
        il.add(new InsnNode(Opcodes.ACONST_NULL));
      }
      il.add(ASMUtils.getPushInstruction(index)); // hook id
      if (listener.requiresArguments()) {
        pushMethodArgsArray(il, mn);
      } else {
        il.add(new InsnNode(Opcodes.ACONST_NULL));
      }
      il.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
          "io/shiftleft/bctrace/runtime/Callback", "onBeforeThrow",
          "(Ljava/lang/Throwable;ILjava/lang/Class;Ljava/lang/Object;I[Ljava/lang/Object;)V",
          false));
    }
    return il;
  }
}
