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

import io.shiftleft.bctrace.Bctrace;
import io.shiftleft.bctrace.asm.util.ASMUtils;
import io.shiftleft.bctrace.runtime.listener.info.BeforeCallSiteListener;
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
import org.objectweb.asm.tree.VarInsnNode;

/**
 * Inserts the bytecode instructions within method node, needed to communicate to the registered
 * listeners, the throwables directly thrown by the target method.
 *
 * This helper turns throw instructions like these:
 * <br><pre>{@code
 * throw aException;
 * }
 * </pre>
 * Into that:
 * <br><pre>{@code
 * // Notify listeners that apply to this method (methodId 1550)
 * Callback.onBeforeThrown(aException, 1550, clazz, this, 0);
 * Callback.onBeforeThrown(aException, 1550, clazz, this, 2);
 * Callback.onBeforeThrown(aException, 1550, clazz, this, 10);
 * ...
 * throw aException;
 * }
 * @author Ignacio del Valle Alles idelvall@shiftleft.io
 */
public class CallSiteHelper extends Helper {


  public static void addByteCodeInstructions(int methodId, ClassNode cn, MethodNode mn,
      ArrayList<Integer> hooksToUse) {

    ArrayList<Integer> listenersToUse = getListenersOfType(hooksToUse,
        BeforeCallSiteListener.class);
    if (!isInstrumentationNeeded(listenersToUse)) {
      return;
    }
    // Auxiliar local variables
    int callSiteInstVarIndex = mn.maxLocals + (ASMUtils.isStatic(mn.access) ? 0 : 1);
    int callSiteArgsVarIndex = callSiteInstVarIndex + 1;
    mn.maxLocals = mn.maxLocals + 2;

    InsnList il = mn.instructions;
    Iterator<AbstractInsnNode> it = il.iterator();
    while (it.hasNext()) {
      AbstractInsnNode node = it.next();
      if (node instanceof MethodInsnNode) {
        MethodInsnNode callSite = (MethodInsnNode) node;
        switch (node.getOpcode()) {
          case Opcodes.INVOKEINTERFACE:
          case Opcodes.INVOKESPECIAL:
          case Opcodes.INVOKEVIRTUAL:
          case Opcodes.INVOKESTATIC:
            il.insertBefore(callSite,
                getCallSiteInstructions(methodId, cn, mn, callSite, listenersToUse,
                    callSiteInstVarIndex, callSiteArgsVarIndex));
            break;
        }
      }
    }
  }

  private static InsnList getCallSiteInstructions(int methodId, ClassNode cn, MethodNode mn,
      MethodInsnNode callSite, ArrayList<Integer> listenersToUse, int callSiteInstVarIndex,
      int callSiteArgsVarIndex) {

    InsnList il = new InsnList();
    boolean matching = false;
    for (Integer index : listenersToUse) {
      BeforeCallSiteListener listener = (BeforeCallSiteListener) Bctrace.getInstance()
          .getHooks()[index].getListener();
      if (listener.getCallSiteClassName().equals(callSite.owner) &&
          listener.getCallSiteMethodName().equals(callSite.name) &&
          listener.getCallSiteMethodDescriptor().equals(callSite.desc)) {
        matching = true;
        break;
      }
    }
    if (!matching) {
      return il;
    }
    il.add(createCallSiteVariables(mn, callSite, callSiteInstVarIndex, callSiteArgsVarIndex));
    for (Integer index : listenersToUse) {
      BeforeCallSiteListener listener = (BeforeCallSiteListener) Bctrace.getInstance()
          .getHooks()[index].getListener();
      if (listener.getCallSiteClassName().equals(callSite.owner) &&
          listener.getCallSiteMethodName().equals(callSite.name) &&
          listener.getCallSiteMethodDescriptor().equals(callSite.desc)) {

        if (callSite.getOpcode() == Opcodes.INVOKESTATIC) { // callsite instance
          il.add(new InsnNode(Opcodes.ACONST_NULL));
        } else {
          il.add(new VarInsnNode(Opcodes.ALOAD, callSiteInstVarIndex)); // call site instance
        }
        il.add(new VarInsnNode(Opcodes.ALOAD, callSiteArgsVarIndex)); // call site args

        il.add(ASMUtils.getPushInstruction(methodId)); // method id
        il.add(getClassConstantReference(Type.getObjectType(cn.name), cn.version)); // class
        if (ASMUtils.isStatic(mn.access) || mn.name.equals("<init>")) { // instance
          il.add(new InsnNode(Opcodes.ACONST_NULL));
        } else {
          il.add(new VarInsnNode(Opcodes.ALOAD, 0));
        }
        il.add(ASMUtils.getPushInstruction(index)); // hook id
        il.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
            "io/shiftleft/bctrace/runtime/Callback", "onBeforeCallSite",
            "(Ljava/lang/Object;[Ljava/lang/Object;ILjava/lang/Class;Ljava/lang/Object;I)V",
            false));
      }
    }
    return il;
  }

  /**
   * Returns the instructions for adding the call site instance and arguments to local variables,
   * maintaining the same stack contents at the end.
   *
   * Preconditions for static call site: stack: ..., arg1,arg2,...,argn ->
   *
   * Preconditions for non-static call site: stack: ..., instance,arg1,arg2,...,argn ->
   */
  private static InsnList createCallSiteVariables(MethodNode mn, MethodInsnNode callSite,
      int callSiteInstanceVarIndex, int callSiteArgsVarIndex) {
    InsnList il = new InsnList();
    Type[] argTypes = Type.getArgumentTypes(callSite.desc);
    il.add(ASMUtils.getPushInstruction(argTypes.length));
    il.add(new TypeInsnNode(Opcodes.ANEWARRAY, "java/lang/Object"));
    il.add(new VarInsnNode(Opcodes.ASTORE, callSiteArgsVarIndex));

    // Store stack values into local var array
    for (int i = argTypes.length - 1; i >= 0; i--) {
      Type argType = argTypes[i];
      il.add(new VarInsnNode(Opcodes.ALOAD, callSiteArgsVarIndex));
      il.add(new InsnNode(Opcodes.SWAP));
      il.add(ASMUtils.getPushInstruction(i));
      il.add(new InsnNode(Opcodes.SWAP));
      MethodInsnNode primitiveToWrapperInst = ASMUtils.getPrimitiveToWrapperInst(argType);
      if (primitiveToWrapperInst != null) {
        il.add(primitiveToWrapperInst);
      }
      il.add(new InsnNode(Opcodes.AASTORE));
    }

    // Store instance
    if (callSite.getOpcode() == Opcodes.INVOKESTATIC) {
      il.add(new InsnNode(Opcodes.ACONST_NULL));
    } else {
      il.add(new InsnNode(Opcodes.DUP));
    }
    il.add(new VarInsnNode(Opcodes.ASTORE, callSiteInstanceVarIndex));

    // Restore stack from array
    for (int i = 0; i < argTypes.length; i++) {
      il.add(new VarInsnNode(Opcodes.ALOAD, callSiteArgsVarIndex));
      il.add(ASMUtils.getPushInstruction(i));
      il.add(new InsnNode(Opcodes.AALOAD));
      String type;
      if (argTypes[i].toString().length() > 1) {
        type = argTypes[i].getInternalName();
      } else {
        type = ASMUtils.getWrapper(argTypes[i]);
      }
      il.add(new TypeInsnNode(Opcodes.CHECKCAST, type));
      MethodInsnNode primitiveToWrapperInst = ASMUtils.getWrapperToPrimitiveInst(argTypes[i]);
      if (primitiveToWrapperInst != null) {
        il.add(primitiveToWrapperInst);
      }
    }
    return il;
  }
}
