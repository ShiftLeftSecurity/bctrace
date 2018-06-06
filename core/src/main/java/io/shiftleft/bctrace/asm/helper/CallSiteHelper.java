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

import io.shiftleft.bctrace.asm.CallbackTransformer;
import io.shiftleft.bctrace.asm.util.ASMUtils;
import io.shiftleft.bctrace.hook.Hook;
import io.shiftleft.bctrace.runtime.listener.specific.CallSiteListener;
import io.shiftleft.bctrace.runtime.listener.specific.DynamicListener.ListenerType;
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
 * Inserts into the method bytecodes, the instructions needed to notify the registered listeners of
 * type CallSiteListener that a call site to the method that each listener is interested in, is
 * about to be executed.
 *
 * Suppose one listener interested in calls to <tt>System.arrayCopy(Object, int, Object, int,
 * int)</tt> Then this helper turns a method with this call:
 * <br><pre>{@code
 * System.arrayCopy(src, 0, target, 0, length);
 * }
 * </pre>
 * Into that:
 * <br><pre>{@code
 * // Notify the event to the listener that apply to this method (suppose methodId 1550)
 * Callback.onBeforeCallSite(null, new Object[]{src, new Integer(0), target, new Integer(0), new
 * Integer(length)}, 1550, clazz, this, listenerIndex);
 * System.arrayCopy(src, 0, target, 0, length);
 * }
 * @author Ignacio del Valle Alles idelvall@shiftleft.io
 */
public class CallSiteHelper extends Helper {

  // Iterates over all instructions and for each call site adds corresponding instructions
  public void addByteCodeInstructions(int methodId, ClassNode cn, MethodNode mn,
      ArrayList<Integer> hooksToUse) {

    // Auxiliar local variables
    int callSiteInstVarIndex = -1;
    if (!ASMUtils.isStatic(mn.access)) {
      callSiteInstVarIndex = mn.maxLocals;
      mn.maxLocals = mn.maxLocals + 1;
    }

    int[][] localVariablesArgumentMap = getLocalVariablesArgumentMap(mn);

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
            InsnList callSiteInstructions = getBeforeCallSiteInstructions(
                methodId,
                cn,
                mn,
                callSite,
                localVariablesArgumentMap,
                callSiteInstVarIndex);
            if (callSiteInstructions != null) {
              il.insertBefore(callSite, callSiteInstructions);
            }
            break;
        }
      }
    }
  }

  private InsnList getBeforeCallSiteInstructions(int methodId, ClassNode cn, MethodNode mn,
      MethodInsnNode callSite, int[][] localVariablesArgumentMap, int callSiteInstVarIndex) {

    Type[] argTypes = Type.getArgumentTypes(callSite.desc);
    boolean staticCall = callSite.getOpcode() == Opcodes.INVOKESTATIC;
    InsnList il = new InsnList();
    for (int i = 0; i < bctrace.getHooks().length; i++) {
      int[] listenerArgs = localVariablesArgumentMap[i];
      if (listenerArgs != null) {
        CallSiteListener listener = (CallSiteListener) bctrace.getHooks()[i].getListener();
        if (listener.getCallSiteClassName().equals(callSite.owner) &&
            listener.getCallSiteMethodName().equals(callSite.name) &&
            listener.getCallSiteMethodDescriptor().equals(callSite.desc)) {

          il.add(createCallSiteVariables(mn, argTypes, staticCall, localVariablesArgumentMap[i],
              callSiteInstVarIndex));

          // Method id, caller class, caller instance, callee instance
          // onBeforeCall(int.class, Class.class, Object.class, Object.class);

          il.add(ASMUtils.getPushInstruction(i)); // hook id
          il.add(ASMUtils.getPushInstruction(methodId)); // method id
          il.add(
              getClassConstantReference(Type.getObjectType(cn.name), cn.version)); // caller class
          if (ASMUtils.isStatic(mn.access) || mn.name.equals("<init>")) { // caller instance
            il.add(new InsnNode(Opcodes.ACONST_NULL));
          } else {
            il.add(new VarInsnNode(Opcodes.ALOAD, 0));
          }
          if (callSite.getOpcode() == Opcodes.INVOKESTATIC) { // callee instance
            il.add(new InsnNode(Opcodes.ACONST_NULL));
          } else {
            il.add(new VarInsnNode(Opcodes.ALOAD, callSiteInstVarIndex));
          }
          // Move local variables to stack
          for (int j = 0; j < argTypes.length; j++) {
            Type argType = argTypes[j];
            il.add(ASMUtils.getLoadInst(argType, localVariablesArgumentMap[i][j]));
          }
          // Invoke dynamically generated callback method. See CallbackTransformer
          il.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
              "io/shiftleft/bctrace/runtime/Callback",
              CallbackTransformer.getDynamicListenerMethodName(listener),
              CallbackTransformer.getDynamicListenerMethodDescriptor(listener),
              false));
        }
      }
    }
    return il;
  }

  /**
   * Returns a int[i][j] holding the indexes for the local variables created for holding the j-th
   * argument of the i-th listener. Updates maxlocals accordingly.
   */
  private int[][] getLocalVariablesArgumentMap(MethodNode mn) {
    Hook[] hooks = bctrace.getHooks();
    int[][] map = new int[hooks.length][];
    for (int i = 0; i < hooks.length; i++) {
      if (!(hooks[i].getListener() instanceof CallSiteListener)) {
        continue;
      }
      CallSiteListener listener = (CallSiteListener) hooks[i].getListener();
      if (listener.getType() != ListenerType.onBeforeCall) {
        continue;
      }
      Iterator<AbstractInsnNode> it = mn.instructions.iterator();
      // We only want to reserve local variables for those listeners that apply to this method:
      while (it.hasNext()) {
        AbstractInsnNode node = it.next();
        if (node instanceof MethodInsnNode) {
          MethodInsnNode callSite = (MethodInsnNode) node;
          switch (node.getOpcode()) {
            case Opcodes.INVOKEINTERFACE:
            case Opcodes.INVOKESPECIAL:
            case Opcodes.INVOKEVIRTUAL:
            case Opcodes.INVOKESTATIC:
              if (listener.getCallSiteClassName().equals(callSite.owner) &&
                  listener.getCallSiteMethodName().equals(callSite.name) &&
                  listener.getCallSiteMethodDescriptor().equals(callSite.desc)) {

                // If here, then this listener will be applied
                Type[] argumentTypes = Type.getArgumentTypes(callSite.desc);
                map[i] = new int[argumentTypes.length];
                for (int j = 0; j < argumentTypes.length; j++) {
                  Type argumentType = argumentTypes[j];
                  map[i][j] = mn.maxLocals;
                  mn.maxLocals = mn.maxLocals + argumentType.getSize();
                }
                break; // continue next listener
              }
          }
        }
      }
    }
    return map;
  }

  /**
   * Returns the instructions for adding the call site instance and arguments to local variables,
   * maintaining the same stack contents at the end.
   *
   * Preconditions for static call site: stack: ..., arg1,arg2,...,argn ->
   *
   * Preconditions for non-static call site: stack: ..., instance,arg1,arg2,...,argn ->
   *
   * @param mn Method node
   * @param argTypes argument types of the current call site
   * @param localVariablesArgumentMap Variable indexes to store each value
   */
  private InsnList createCallSiteVariables(MethodNode mn, Type[] argTypes, boolean staticCall,
      int[] localVariablesArgumentMap, int callSiteInstVarIndex) {

    InsnList il = new InsnList();

    // Store stack values into local var array
    for (int i = argTypes.length - 1; i >= 0; i--) {
      Type argType = argTypes[i];
      il.add(ASMUtils.getStoreInst(argType, localVariablesArgumentMap[i]));
    }
    // Store instance
    if (staticCall) {
      il.add(new InsnNode(Opcodes.ACONST_NULL));
    } else {
      il.add(new InsnNode(Opcodes.DUP));
    }
    il.add(new VarInsnNode(Opcodes.ASTORE, callSiteInstVarIndex));

    // Restore stack from local variables
    for (int i = 0; i < argTypes.length; i++) {
      Type argType = argTypes[i];
      il.add(ASMUtils.getLoadInst(argType, localVariablesArgumentMap[i]));
    }
    return il;
  }
}
