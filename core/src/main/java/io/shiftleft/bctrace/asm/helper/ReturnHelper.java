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
import io.shiftleft.bctrace.runtime.listener.info.FinishReturnArgumentsListener;
import io.shiftleft.bctrace.runtime.listener.info.FinishReturnListener;
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
import org.objectweb.asm.tree.VarInsnNode;

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
 *   Callback.onFinishedReturn(ret, clazz, this, 0, arg);
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


  public static void addByteCodeInstructions(int methodId, ClassNode cn, MethodNode mn,
      ArrayList<Integer> hooksToUse) {

    addReturnTraceWithoutArguments(methodId, cn, mn, hooksToUse);
    addReturnTraceWithArguments(methodId, cn, mn, hooksToUse);
  }

  private static void addReturnTraceWithoutArguments(int methodId, ClassNode cn, MethodNode mn,
      ArrayList<Integer> hooksToUse) {
    ArrayList<Integer> listenersToUse = getListenersOfType(hooksToUse, FinishReturnListener.class);
    if (!isInstrumentationNeeded(listenersToUse)) {
      return;
    }
    addReturnTrace(methodId, cn, mn, listenersToUse, false);
  }

  private static void addReturnTraceWithArguments(int methodId, ClassNode cn, MethodNode mn,
      ArrayList<Integer> hooksToUse) {
    ArrayList<Integer> listenersToUse = getListenersOfType(hooksToUse,
        FinishReturnArgumentsListener.class);
    if (!isInstrumentationNeeded(listenersToUse)) {
      return;
    }
    addReturnTrace(methodId, cn, mn, listenersToUse, true);
  }

  private static void addReturnTrace(int methodId, ClassNode cn, MethodNode mn,
      ArrayList<Integer> listenersToUse, boolean passArguments) {
    InsnList il = mn.instructions;
    Iterator<AbstractInsnNode> it = il.iterator();
    Type returnType = Type.getReturnType(mn.desc);

    while (it.hasNext()) {
      AbstractInsnNode abstractInsnNode = it.next();

      switch (abstractInsnNode.getOpcode()) {
        case Opcodes.RETURN:
          il.insertBefore(abstractInsnNode,
              getVoidReturnTraceInstructions(methodId, cn, mn, listenersToUse, passArguments));
          break;
        case Opcodes.IRETURN:
        case Opcodes.LRETURN:
        case Opcodes.FRETURN:
        case Opcodes.ARETURN:
        case Opcodes.DRETURN:
          il.insertBefore(abstractInsnNode,
              getReturnTraceInstructions(methodId, cn, mn, returnType, listenersToUse,
                  passArguments));
      }
    }
  }

  private static InsnList getVoidReturnTraceInstructions(int methodId, ClassNode cn, MethodNode mn,
      ArrayList<Integer> listenersToUse, boolean passArguments) {
    InsnList il = new InsnList();
    for (int i = listenersToUse.size() - 1; i >= 0; i--) {
      Integer index = listenersToUse.get(i);
      il.add(new InsnNode(Opcodes.ACONST_NULL)); // return value
      il.add(ASMUtils.getPushInstruction(methodId)); // method id
      il.add(getClassConstantReference(Type.getObjectType(cn.name), cn.version)); // class
      if (ASMUtils.isStatic(mn.access) || mn.name.equals("<init>")) { // current instance
        il.add(new InsnNode(Opcodes.ACONST_NULL));
      } else {
        il.add(new VarInsnNode(Opcodes.ALOAD, 0));
      }
      il.add(ASMUtils.getPushInstruction(index)); // hook id

      if (passArguments) {
        pushMethodArgsArray(il, mn);
        il.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
            "io/shiftleft/bctrace/runtime/Callback", "onFinishedReturn",
            "(Ljava/lang/Object;ILjava/lang/Class;Ljava/lang/Object;I[Ljava/lang/Object;)V",
            false));
      } else {
        il.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
            "io/shiftleft/bctrace/runtime/Callback", "onFinishedReturn",
            "(Ljava/lang/Object;ILjava/lang/Class;Ljava/lang/Object;I)V", false));
      }
    }
    return il;
  }

  private static InsnList getReturnTraceInstructions(int methodId, ClassNode cn, MethodNode mn,
      Type returnType, ArrayList<Integer> listenersToUse, boolean passArguments) {
    InsnList il = new InsnList();
    for (int i = listenersToUse.size() - 1; i >= 0; i--) {
      Integer index = listenersToUse.get(i);
      if (returnType.getSize() == 1) {
        il.add(new InsnNode(Opcodes.DUP));
      } else {
        il.add(new InsnNode(Opcodes.DUP2));
      }
      MethodInsnNode primitiveToWrapperInst = ASMUtils.getPrimitiveToWrapperInst(returnType);
      if (primitiveToWrapperInst != null) {
        il.add(primitiveToWrapperInst);
      }
      il.add(ASMUtils.getPushInstruction(methodId)); // method id
      il.add(getClassConstantReference(Type.getObjectType(cn.name), cn.version)); // class
      if (ASMUtils.isStatic(mn.access) || mn.name.equals("<init>")) { // current instance
        il.add(new InsnNode(Opcodes.ACONST_NULL));
      } else {
        il.add(new VarInsnNode(Opcodes.ALOAD, 0));
      }
      il.add(ASMUtils.getPushInstruction(index)); // hook id
      if (passArguments) {
        pushMethodArgsArray(il, mn);
        il.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
            "io/shiftleft/bctrace/runtime/Callback", "onFinishedReturn",
            "(Ljava/lang/Object;ILjava/lang/Class;Ljava/lang/Object;I[Ljava/lang/Object;)V",
            false));
      } else {
        il.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
            "io/shiftleft/bctrace/runtime/Callback", "onFinishedReturn",
            "(Ljava/lang/Object;ILjava/lang/Class;Ljava/lang/Object;I)V", false));
      }
    }
    return il;
  }
}
