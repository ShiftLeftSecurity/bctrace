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
import io.shiftleft.bctrace.runtime.listener.generic.GenericMethodReturnListener;
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
import org.objectweb.asm.tree.TypeInsnNode;

/**
 * Inserts the bytecode instructions within method node, needed to handle the return listeners
 * registered.
 *
 * This primitive turns the method node instructions of a method like this:
 * <br><pre>{@code
 * public Object foo(Object arg){
 *   return void(arg);
 * }
 * }
 * </pre>
 * Into that:
 * <br><pre>{@code
 * public Object foo(Object arg){
 *   try{
 *     Object ret = void(arg);
 *     // Notify listeners that apply to this method
 *     ret = Callback.onFinish(ret, ret, null, clazz, this, 0, arg);
 *     ret = Callback.onFinish(ret, ret, null, clazz, this, 2, arg);
 *     ret = Callback.onFinish(ret, ret, null, clazz, this, 10, arg);
 *     // Return the original value
 *     return ret;
 *   } catch (Throwable th){
 *     th = Callback.onFinish(th, null, th, clazz, this, 0, arg);
 *     th = Callback.onFinish(th, null, th, clazz, this, 2, arg);
 *     th = Callback.onFinish(th, null, th, clazz, this, 10, arg);
 *     throw th;
 *   }
 * }
 * </pre>
 *
 * @author Ignacio del Valle Alles idelvall@shiftleft.io
 */
public class GenericMethodReturnPrimitive extends InstrumentationPrimitive {

  @Override
  public boolean addByteCodeInstructions(int methodId, ClassNode cn, MethodNode mn,
      ArrayList<Integer> hooksToUse) {

    ArrayList<Integer> listenersToUse = getListenersOfType(hooksToUse,
        GenericMethodReturnListener.class);

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

    while (it.hasNext()) {
      AbstractInsnNode abstractInsnNode = it.next();

      switch (abstractInsnNode.getOpcode()) {
        case Opcodes.RETURN:
        case Opcodes.IRETURN:
        case Opcodes.LRETURN:
        case Opcodes.FRETURN:
        case Opcodes.ARETURN:
        case Opcodes.DRETURN:
          il.insertBefore(abstractInsnNode,
              getReturnInstructions(methodId, cn, mn, listenersToUse));
      }
    }
  }

  private InsnList getReturnInstructions(int methodId, ClassNode cn, MethodNode mn,
      ArrayList<Integer> listenersToUse) {
    Type returnType = Type.getReturnType(mn.desc);
    InsnList il = new InsnList();
    // Auxiliar local variables
    int returnVarIndex = mn.maxLocals;
    if (!returnType.getDescriptor().equals("V")) {
      mn.maxLocals = mn.maxLocals + returnType.getSize();
      // Store original return value into a local variable
      il.add(ASMUtils.getStoreInst(returnType, returnVarIndex));
    }
    for (int i = listenersToUse.size() - 1; i >= 0; i--) {
      Integer index = listenersToUse.get(i);
      GenericMethodReturnListener listener = (GenericMethodReturnListener) bctrace.getHooks()[index]
          .getListener();
      if (!returnType.getDescriptor().equals("V")) {
        il.add(ASMUtils
            .getLoadInst(returnType, returnVarIndex)); // Pop original return value to the stack
        MethodInsnNode primitiveToWrapperInst = ASMUtils.getPrimitiveToWrapperInst(returnType);
        if (primitiveToWrapperInst != null) {
          il.add(primitiveToWrapperInst);
        }
      } else {
        il.add(new InsnNode(Opcodes.ACONST_NULL));
      }
      il.add(ASMUtils.getPushInstruction(methodId)); // method id
      il.add(getClassConstantReference(Type.getObjectType(cn.name), cn.version)); // class
      pushInstance(il, mn, true); // current instance
      il.add(ASMUtils.getPushInstruction(index)); // hook id
      pushMethodArgsArray(il, mn);
      il.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
          "io/shiftleft/bctrace/runtime/Callback", "onReturn",
          "(Ljava/lang/Object;ILjava/lang/Class;Ljava/lang/Object;I[Ljava/lang/Object;)Ljava/lang/Object;",
          false));

      if (returnType.getDescriptor().equals("V")) {
        il.add(new InsnNode(Opcodes.POP));
      } else {
        String castType = returnType.getInternalName();
        String wrapperType = ASMUtils.getWrapper(returnType);
        if (wrapperType != null) {
          castType = wrapperType;
        }
        il.add(new TypeInsnNode(Opcodes.CHECKCAST, castType));
        MethodInsnNode wrapperToPrimitiveInst = ASMUtils.getWrapperToPrimitiveInst(returnType);
        if (wrapperToPrimitiveInst != null) {
          il.add(wrapperToPrimitiveInst);
        }
        if (returnType.getSize() == 1) {
          il.add(new InsnNode(Opcodes.DUP));
        } else {
          il.add(new InsnNode(Opcodes.DUP2));
        }
        // Update return value local variable, so each listener receives the modified value from the ones before
        // instead of getting all of them the original value
        il.add(ASMUtils.getStoreInst(returnType, returnVarIndex));
      }
    }
    return il;
  }

}
