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
import io.shiftleft.bctrace.runtime.listener.info.StartArgumentsListener;
import io.shiftleft.bctrace.runtime.listener.info.StartListener;
import io.shiftleft.bctrace.runtime.listener.min.MinStartListener;
import io.shiftleft.bctrace.runtime.listener.mut.StartMutableListener;
import java.util.ArrayList;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

/**
 * Inserts the bytecode instructions within method node, needed to handle the different start
 * listeners registered.
 *
 * @author Ignacio del Valle Alles idelvall@shiftleft.io
 */
public class StartHelper extends Helper {

  public static void addByteCodeInstructions(int methodId, ClassNode cn, MethodNode mn,
      ArrayList<Integer> hooksToUse) {

    addMinTraceStart(methodId, cn, mn, hooksToUse);
    addTraceStart(methodId, cn, mn, hooksToUse);
    addTraceStartWithArguments(methodId, cn, mn, hooksToUse);
    addMutableTraceStartWithArguments(methodId, cn, mn, hooksToUse);
  }

  /**
   * Depending on the {@link MinStartListener} listeners that apply to this method,  this helper
   * turns the method node instructions of a method like this:
   * <br><pre>{@code
   * public Object foo(Object arg){
   *   return void(args);
   * }
   * }
   * </pre>
   * Into that:
   * <br><pre>{@code
   * public Object foo(Object arg){
   *   Object ret = void(args);
   *   // Notify listeners that apply to this method (methodId 1550)
   *   Callback.onStart(1550, 0);
   *   Callback.onStart(1550, 2);
   *   Callback.onStart(1550, 10);
   *   return void(args);
   * }
   * }
   * </pre>
   */
  private static void addMinTraceStart(int methodId, ClassNode cn, MethodNode mn,
      ArrayList<Integer> hooksToUse) {
    ArrayList<Integer> listenersToUse = getListenersOfType(hooksToUse, MinStartListener.class);
    if (!isInstrumentationNeeded(listenersToUse)) {
      return;
    }
    InsnList il = new InsnList();
    for (Integer index : listenersToUse) {
      il.add(ASMUtils.getPushInstruction(methodId));
      il.add(ASMUtils.getPushInstruction(index));
      il.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
          "io/shiftleft/bctrace/runtime/Callback", "onStart",
          "(II)V", false));
    }
    mn.instructions.insert(il);
  }

  /**
   * Depending on the {@link StartListener} listeners that apply to this method,  this helper turns
   * the method node instructions of a method like this:
   * <br><pre>{@code
   * public Object foo(Object args){
   *   return void(args);
   * }
   * }
   * </pre>
   * Into that:
   * <br><pre>{@code
   * public Object foo(Object arg){
   *   Object ret = void(arg);
   *   // Notify listeners that apply to this method (methodId 1550)
   *   Callback.onStart(1550, this, 0);
   *   Callback.onStart(1550, this, 2);
   *   Callback.onStart(1550, this, 10);
   *   return void(args);
   * }
   * }
   * </pre>
   */
  private static void addTraceStart(int methodId, ClassNode cn, MethodNode mn,
      ArrayList<Integer> hooksToUse) {
    ArrayList<Integer> listenersToUse = getListenersOfType(hooksToUse, StartListener.class);
    if (!isInstrumentationNeeded(listenersToUse)) {
      return;
    }
    InsnList il = new InsnList();
    for (Integer index : listenersToUse) {
      il.add(ASMUtils.getPushInstruction(methodId));
      if (ASMUtils.isStatic(mn.access) || mn.name.equals("<init>")) {
        il.add(new InsnNode(Opcodes.ACONST_NULL));
      } else {
        il.add(new VarInsnNode(Opcodes.ALOAD, 0));
      }
      il.add(ASMUtils.getPushInstruction(index));
      il.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
          "io/shiftleft/bctrace/runtime/Callback", "onStart",
          "(ILjava/lang/Object;I)V", false));
    }
    mn.instructions.insert(il);
  }

  /**
   * Depending on the {@link StartArgumentsListener} listeners that apply to this method,  this
   * helper turns the method node instructions of a method like this:
   * <br><pre>{@code
   * public Object foo(Object arg1, Object arg2, ..., Object argn){
   *   return void(arg1, arg2, ..., argn);
   * }
   * }
   * </pre>
   * Into that:
   * <br><pre>{@code
   * public Object foo(Object args){
   *   Object[] args = new Object[]{arg1, arg2, ..., argn};
   *   Object ret = void(args);
   *   // Notify listeners that apply to this method (methodId 1550)
   *   Callback.onStart(args, 1550, this, 0);
   *   Callback.onStart(args, 1550, this, 2);
   *   Callback.onStart(args, 1550, this, 10);
   *   return void(arg1, arg2, ..., argn);
   * }
   * }
   * </pre>
   */
  private static void addTraceStartWithArguments(int methodId, ClassNode cn, MethodNode mn,
      ArrayList<Integer> hooksToUse) {
    ArrayList<Integer> listenersToUse = getListenersOfType(hooksToUse,
        StartArgumentsListener.class);
    if (!isInstrumentationNeeded(listenersToUse)) {
      return;
    }
    InsnList il = new InsnList();
    addMethodParametersVariable(il, mn);

    for (int i = 0; i < listenersToUse.size(); i++) {
      Integer index = listenersToUse.get(i);
      if (i < listenersToUse.size() - 1) {
        il.add(new InsnNode(Opcodes.DUP));
      }
      il.add(ASMUtils.getPushInstruction(methodId));
      if (ASMUtils.isStatic(mn.access) || mn.name.equals("<init>")) {
        il.add(new InsnNode(Opcodes.ACONST_NULL));
      } else {
        il.add(new VarInsnNode(Opcodes.ALOAD, 0));
      }
      il.add(ASMUtils.getPushInstruction(index));
      il.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
          "io/shiftleft/bctrace/runtime/Callback", "onStart",
          "([Ljava/lang/Object;ILjava/lang/Object;I)V", false));
    }
    mn.instructions.insert(il);
  }

  /**
   * Depending on the {@link StartMutableListener} listeners that apply to this method,  this helper
   * turns the method node instructions of a method like this:
   * <br><pre>{@code
   * public Object foo(Object arg1, Object arg2, ..., Object argn){
   *   return void(arg1, arg2, ..., argn);
   * }
   * }
   * </pre>
   * Into that:
   * <br><pre>{@code
   * public Object foo(Object args){
   *   Object[] args = new Object[]{arg1, arg2, ..., argn};
   *   Object ret = void(args);
   *   // Notify listeners that apply to this method (methodId 1550)
   *   Return ret0 = Callback.onStart(args, 1550, this, 0);
   *   if(ret0 != null){
   *     return ret0.value;
   *   }
   *   Callback.onStart(args, 1550, this, 2);
   *   if(ret2 != null){
   *     return ret2.value;
   *   }
   *   Callback.onStart(args, 1550, this, 10);
   *   if(ret10 != null){
   *     return ret10.value;
   *   }
   *   return void(arg1, arg2, ..., argn);
   * }
   * }
   * </pre>
   */
  private static void addMutableTraceStartWithArguments(int methodId, ClassNode cn, MethodNode mn,
      ArrayList<Integer> hooksToUse) {
    ArrayList<Integer> listenersToUse = getListenersOfType(hooksToUse, StartMutableListener.class);
    if (!isInstrumentationNeeded(listenersToUse)) {
      return;
    }
    if (mn.name.equals("<init>")) {
      throw new UnsupportedOperationException("Skipping constructor execution is not supported");
    }
    InsnList il = new InsnList();
    addMethodParametersVariable(il, mn);

    for (int i = 0; i < listenersToUse.size(); i++) {
      Integer index = listenersToUse.get(i);
      if (i < listenersToUse.size() - 1) {
        il.add(new InsnNode(Opcodes.DUP));
      }
      il.add(ASMUtils.getPushInstruction(methodId));
      if (ASMUtils.isStatic(mn.access) || mn.name.equals("<init>")) {
        il.add(new InsnNode(Opcodes.ACONST_NULL));
      } else {
        il.add(new VarInsnNode(Opcodes.ALOAD, 0));
      }
      il.add(ASMUtils.getPushInstruction(index));
      il.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
          "io/shiftleft/bctrace/runtime/Callback", "onMutableStart",
          "([Ljava/lang/Object;ILjava/lang/Object;I)Lio/shiftleft/bctrace/runtime/listener/mut/StartMutableListener$Return;",
          false));
      il.add(new InsnNode(Opcodes.DUP));
      LabelNode returnBlock = new LabelNode();
      il.add(new JumpInsnNode(Opcodes.IFNULL, returnBlock));
      Type returnType = Type.getReturnType(mn.desc);
      if (returnType.getDescriptor().equals("V")) {
        il.add(new InsnNode(Opcodes.POP));
        il.add(new InsnNode(Opcodes.RETURN));
      } else {
        il.add(new FieldInsnNode(Opcodes.GETFIELD,
            "io/shiftleft/bctrace/runtime/listener/mut/StartMutableListener$Return", "value",
            "Ljava/lang/Object;"));
        MethodInsnNode wrapperToPrimitiveInst = ASMUtils.getWrapperToPrimitiveInst(returnType);
        if (wrapperToPrimitiveInst != null) {
          il.add(new TypeInsnNode(Opcodes.CHECKCAST, ASMUtils.getWrapper(returnType)));
          il.add(wrapperToPrimitiveInst);
          il.add(ASMUtils.getReturnInst(returnType));
        } else {
          il.add(new TypeInsnNode(Opcodes.CHECKCAST, returnType.getInternalName()));
          il.add(new InsnNode(Opcodes.ARETURN));
        }
      }
      il.add(returnBlock);
      il.add(new InsnNode(Opcodes.POP));

    }
    mn.instructions.insert(il);
  }

  /**
   * Creates the parameter object array reference on top of the operand stack
   */
  private static void addMethodParametersVariable(InsnList il, MethodNode mn) {
    Type[] methodArguments = Type.getArgumentTypes(mn.desc);
    if (methodArguments.length == 0) {
      il.add(new InsnNode(Opcodes.ACONST_NULL));
    } else {
      il.add(ASMUtils.getPushInstruction(methodArguments.length));
      il.add(new TypeInsnNode(Opcodes.ANEWARRAY, "java/lang/Object"));
      int index = ASMUtils.isStatic(mn.access) ? 0 : 1;
      for (int i = 0; i < methodArguments.length; i++) {
        il.add(new InsnNode(Opcodes.DUP));
        il.add(ASMUtils.getPushInstruction(i));
        il.add(ASMUtils.getLoadInst(methodArguments[i], index));
        MethodInsnNode primitiveToWrapperInst = ASMUtils
            .getPrimitiveToWrapperInst(methodArguments[i]);
        if (primitiveToWrapperInst != null) {
          il.add(primitiveToWrapperInst);
        }
        il.add(new InsnNode(Opcodes.AASTORE));
        index += methodArguments[i].getSize();
      }
    }
  }
}
