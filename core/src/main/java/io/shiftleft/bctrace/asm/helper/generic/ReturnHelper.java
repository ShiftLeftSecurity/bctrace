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
import io.shiftleft.bctrace.runtime.listener.generic.Disabled;
import io.shiftleft.bctrace.runtime.listener.generic.ReturnListener;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
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
 * Inserts the bytecode instructions within method node, needed to handle the return listeners
 * registered.
 *
 * This helper turns the method node instructions of a method like this:
 * <br><pre>{@code
 * public Object foo(Object arg){
 *   return void(arg);
 * }
 * }
 * </pre>
 * Into that:
 * <br><pre>{@code
 * public Object foo(Object arg){
 *   Object ret = void(arg);
 *   // Notify listeners that apply to this method
 *   Callback.onFinish(ret, clazz, this, 0, arg);
 *   Callback.onFinishedThrowable(ret, clazz, this, 2, arg);
 *   Callback.onFinishedThrowable(ret, clazz, this, 10, arg);
 *   // Return the original value
 *   return ret;
 * }
 * }
 * </pre>
 *
 * @author Ignacio del Valle Alles idelvall@shiftleft.io
 */
public class ReturnHelper extends Helper {


  public boolean addByteCodeInstructions(int methodId, ClassNode cn, MethodNode mn,
      ArrayList<Integer> hooksToUse) {

    return addReturnTraceWithArguments(methodId, cn, mn, hooksToUse);
  }

  private boolean addReturnTraceWithArguments(int methodId, ClassNode cn, MethodNode mn,
      ArrayList<Integer> hooksToUse) {
    ArrayList<Integer> listenersToUse = getListenersOfType(hooksToUse,
        ReturnListener.class);
    if (!isInstrumentationNeeded(listenersToUse)) {
      return false;
    }
    addReturnTrace(methodId, cn, mn, listenersToUse);
    return true;
  }

  private void addReturnTrace(int methodId, ClassNode cn, MethodNode mn,
      ArrayList<Integer> listenersToUse) {
    InsnList il = mn.instructions;
    Iterator<AbstractInsnNode> it = il.iterator();
    Type returnType = Type.getReturnType(mn.desc);

    while (it.hasNext()) {
      AbstractInsnNode abstractInsnNode = it.next();

      switch (abstractInsnNode.getOpcode()) {
        case Opcodes.RETURN:
          il.insertBefore(abstractInsnNode,
              getVoidReturnTraceInstructions(methodId, cn, mn, listenersToUse));
          break;
        case Opcodes.IRETURN:
        case Opcodes.LRETURN:
        case Opcodes.FRETURN:
        case Opcodes.ARETURN:
        case Opcodes.DRETURN:
          il.insertBefore(abstractInsnNode,
              getReturnTraceInstructions(methodId, cn, mn, returnType, listenersToUse));
      }
    }
  }

  private InsnList getVoidReturnTraceInstructions(int methodId, ClassNode cn, MethodNode mn,
      ArrayList<Integer> listenersToUse) {
    InsnList il = new InsnList();
    for (int i = listenersToUse.size() - 1; i >= 0; i--) {
      Integer index = listenersToUse.get(i);
      ReturnListener listener = (ReturnListener) bctrace.getHooks()[index].getListener();
      il.add(new InsnNode(Opcodes.ACONST_NULL)); // return value
      if (listener.requiresMethodId()) {
        il.add(ASMUtils.getPushInstruction(methodId)); // method id
      } else {
        il.add(ASMUtils.getPushInstruction(0));
      }
      if (listener.requiresClass()) {
        il.add(getClassConstantReference(Type.getObjectType(cn.name), cn.version)); // class
      } else {
        il.add(new InsnNode(Opcodes.ACONST_NULL));
      }
      if (listener.requiresInstance()) {
        pushInstance(il, mn, true); // current instance
      } else {
        il.add(new InsnNode(Opcodes.ACONST_NULL));
      }
      il.add(ASMUtils.getPushInstruction(index)); // hook id
      pushMethodArgsArray(il, mn);
      il.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
          "io/shiftleft/bctrace/runtime/Callback", "onReturn",
          "(Ljava/lang/Object;ILjava/lang/Class;Ljava/lang/Object;I[Ljava/lang/Object;)V",
          false));
    }
    return il;
  }

  private InsnList getReturnTraceInstructions(int methodId, ClassNode cn, MethodNode mn,
      Type returnType, ArrayList<Integer> listenersToUse) {
    InsnList il = new InsnList();
    for (int i = listenersToUse.size() - 1; i >= 0; i--) {
      Integer index = listenersToUse.get(i);
      ReturnListener listener = (ReturnListener) bctrace.getHooks()[index].getListener();
      if (listener.requiresReturnValue()) {
        if (returnType.getSize() == 1) {
          il.add(new InsnNode(Opcodes.DUP));
        } else {
          il.add(new InsnNode(Opcodes.DUP2));
        }
        MethodInsnNode primitiveToWrapperInst = ASMUtils.getPrimitiveToWrapperInst(returnType);
        if (primitiveToWrapperInst != null) {
          il.add(primitiveToWrapperInst);
        }
      } else {
        il.add(new InsnNode(Opcodes.ACONST_NULL));
      }
      if (listener.requiresMethodId()) {
        il.add(ASMUtils.getPushInstruction(methodId)); // method id
      } else {
        il.add(ASMUtils.getPushInstruction(0));
      }
      if (listener.requiresClass()) {
        il.add(getClassConstantReference(Type.getObjectType(cn.name), cn.version)); // class
      } else {
        il.add(new InsnNode(Opcodes.ACONST_NULL));
      }
      if (listener.requiresInstance()) {
        pushInstance(il, mn, true); // current instance
      } else {
        il.add(new InsnNode(Opcodes.ACONST_NULL));
      }
      il.add(ASMUtils.getPushInstruction(index)); // hook id
      pushMethodArgsArray(il, mn);
      il.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
          "io/shiftleft/bctrace/runtime/Callback", "onReturn",
          "(Ljava/lang/Object;ILjava/lang/Class;Ljava/lang/Object;I[Ljava/lang/Object;)V",
          false));
    }
    return il;
  }
}
