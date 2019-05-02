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
 * of this source code, which includes information that is confidential and/or proprietary, and
 * is a trade secret, of ShiftLeft, Inc.
 *
 * ANY REPRODUCTION, MODIFICATION, DISTRIBUTION, PUBLIC PERFORMANCE, OR PUBLIC DISPLAY
 * OF OR THROUGH USE OF THIS SOURCE CODE WITHOUT THE EXPRESS WRITTEN CONSENT OF ShiftLeft, Inc.
 * IS STRICTLY PROHIBITED, AND IN VIOLATION OF APPLICABLE LAWS AND INTERNATIONAL TREATIES.
 * THE RECEIPT OR POSSESSION OF THIS SOURCE CODE AND/OR RELATED INFORMATION DOES NOT
 * CONVEY OR IMPLY ANY RIGHTS TO REPRODUCE, DISCLOSE OR DISTRIBUTE ITS
 * CONTENTS, OR TO MANUFACTURE, USE, OR SELL ANYTHING THAT IT MAY DESCRIBE, IN WHOLE OR IN PART.
 */
package io.shiftleft.bctrace.asm.primitive.generic.method;

import io.shiftleft.bctrace.asm.primitive.InstrumentationPrimitive;
import io.shiftleft.bctrace.asm.util.ASMUtils;
import io.shiftleft.bctrace.runtime.listener.generic.GenericMethodMutableStartListener;
import java.util.ArrayList;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;

/**
 * Inserts the bytecode instructions within method node, needed to handle the different start
 * listeners registered.
 *
 * @author Ignacio del Valle Alles idelvall@shiftleft.io
 */
public class GenericMethodMutableStartPrimitive extends InstrumentationPrimitive {

  @Override
  public boolean addByteCodeInstructions(int methodId, ClassNode cn, MethodNode mn,
      ArrayList<Integer> hooksToUse) {

    return addMutableTraceStart(methodId, cn, mn, hooksToUse);
  }

  /**
   * Depending on the {@link GenericMethodMutableStartListener} listeners that apply to this method,
   * this primitive turns the method node instructions of a method like this:
   * <br><pre>{@code
   * public Object foo(Object arg1, Object arg2, ..., Object argn){
   *   return void(arg1, arg2, ..., argn);
   * }
   * }
   * </pre>
   * Into that:int methodId
   * <br><pre>{@code
   * public Object foo(Object args){
   *   Object[] args = new Object[]{arg1, arg2, ..., argn};
   *   // Notify listeners that apply to this method (methodId 1550)
   *   args = Callback.onStart(args, 1550, clazz, this, 0);
   *   args = Callback.onStart(args, 1550, clazz, this, 2);
   *   args = Callback.onStart(args, 1550, clazz, this, 10);
   *   return void(arg1, arg2, ..., argn);
   * }
   * }
   * </pre>
   */
  private boolean addMutableTraceStart(int methodId, ClassNode cn, MethodNode mn,
      ArrayList<Integer> hooksToUse) {
    ArrayList<Integer> listenersToUse = getListenersOfType(hooksToUse,
        GenericMethodMutableStartListener.class);
    if (!isInstrumentationNeeded(listenersToUse)) {
      return false;
    }
    InsnList il = new InsnList();
    Type[] methodArguments = Type.getArgumentTypes(mn.desc);
    boolean someRequiresArguments = false;
    for (int i = 0; i < listenersToUse.size(); i++) {
      Integer index = listenersToUse.get(i);
      GenericMethodMutableStartListener listener = (GenericMethodMutableStartListener) bctrace
          .getHooks()[index]
          .getListener();
      if (listener.requiresArguments()) {
        someRequiresArguments = true;
        break;
      }
    }
    if (someRequiresArguments) {
      pushMethodArgsArray(il, mn);
    } else {
      il.add(new InsnNode(Opcodes.ACONST_NULL));
    }
    for (int i = 0; i < listenersToUse.size(); i++) {
      Integer index = listenersToUse.get(i);
      GenericMethodMutableStartListener listener = (GenericMethodMutableStartListener) bctrace
          .getHooks()[index]
          .getListener();
      il.add(ASMUtils.getPushInstruction(methodId));
      il.add(getClassConstantReference(Type.getObjectType(cn.name), cn.version)); // class
      pushInstance(il, mn); // current instance
      il.add(ASMUtils.getPushInstruction(index));
      il.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
          "io/shiftleft/bctrace/runtime/Callback", "onMutableStart",
          "([Ljava/lang/Object;ILjava/lang/Class;Ljava/lang/Object;I)[Ljava/lang/Object;", false));
      overwriteMethodArgsFromArray(il, mn);
    }
    il.add(new InsnNode(Opcodes.POP));
    mn.instructions.insert(il);
    return true;
  }

  /**
   * Overwrites the argument local variables from the array in top of the operand stack
   */
  protected void overwriteMethodArgsFromArray(InsnList il, MethodNode mn) {
    Type[] methodArguments = Type.getArgumentTypes(mn.desc);
    if (methodArguments.length > 0) {
      int index = ASMUtils.isStatic(mn.access) ? 0 : 1;
      for (int i = 0; i < methodArguments.length; i++) {
        il.add(new InsnNode(Opcodes.DUP));
        il.add(ASMUtils.getPushInstruction(i));
        il.add(new InsnNode(Opcodes.AALOAD));
        MethodInsnNode wrapperToPrimitiveInst = ASMUtils
            .getWrapperToPrimitiveInst(methodArguments[i]);
        String castType = methodArguments[i].getInternalName();
        String wrapperType = ASMUtils.getWrapper(methodArguments[i]);
        if (wrapperType != null) {
          castType = wrapperType;
        }
        il.add(new TypeInsnNode(Opcodes.CHECKCAST, castType));
        if (wrapperToPrimitiveInst != null) {
          il.add(wrapperToPrimitiveInst);
        }
        il.add(ASMUtils.getStoreInst(methodArguments[i], index));
        index += methodArguments[i].getSize();
      }
    }
  }
}


