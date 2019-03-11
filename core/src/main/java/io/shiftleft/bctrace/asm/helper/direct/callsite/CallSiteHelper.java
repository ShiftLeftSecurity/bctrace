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
package io.shiftleft.bctrace.asm.helper.direct.callsite;

import io.shiftleft.bctrace.asm.CallbackTransformer;
import io.shiftleft.bctrace.asm.helper.Helper;
import io.shiftleft.bctrace.asm.util.ASMUtils;
import io.shiftleft.bctrace.filter.CallSiteFilter;
import io.shiftleft.bctrace.hook.Hook;
import io.shiftleft.bctrace.runtime.listener.direct.DirectCallSiteReturnListener;
import io.shiftleft.bctrace.runtime.listener.direct.DirectCallSiteStartListener;
import io.shiftleft.bctrace.runtime.listener.direct.DirectCallSiteThrowableListener;
import io.shiftleft.bctrace.runtime.listener.direct.DirectListener;
import java.util.ArrayList;
import java.util.Iterator;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import org.objectweb.asm.tree.VarInsnNode;

/**
 * Inserts into the method bytecodes, the instructions needed to notify the registered listeners of
 * type DirectCallSiteListener that a call site to the method that each listener is interested in,
 * is about to be executed.
 *
 * Suppose one listener interested in calls to <tt>System.arrayCopy(Object, int, Object, int,
 * int)</tt> Then this helper turns a method with this call:
 * <br><pre>{@code
 * System.arrayCopy(src, 0, target, 0, length);
 * }
 * </pre>
 * Into that:
 * <br><pre>{@code
 * // Notify the event to the listener that apply to this method
 * Callback.onBeforeCallSite(listenerIndex, callerClass, callerInstance, null, src, 0, target, 0,
 * length);
 * System.arrayCopy(src, 0, target, 0, length);
 * }
 * @author Ignacio del Valle Alles idelvall@shiftleft.io
 */
public class CallSiteHelper extends Helper {

  /**
   * Iterates over all instructions and for each call site adds corresponding instructions
   *
   * @return true if the method has been transformed. False otherwise
   */
  public boolean addByteCodeInstructions(ClassNode cn, MethodNode mn,
      ArrayList<Integer> hooksToUse) {

    // Helper local variables
    int callSiteInstanceVarIndex = mn.maxLocals;
    mn.maxLocals = mn.maxLocals + 1;
    int throwableVarIndex = mn.maxLocals;
    mn.maxLocals = mn.maxLocals + 1;
    // Could optimized to reuse based on return type (different called methods with same return type)
    int[] returnVariablesMap = getReturnVariablesArgumentMap(cn, mn, hooksToUse);
    int[][] localVariablesArgumentMap = getLocalVariablesArgumentMap(cn, mn, hooksToUse);

    InsnList il = mn.instructions;
    Iterator<AbstractInsnNode> it = il.iterator();
    boolean ret = false;
    int lineNumber = -1;
    while (it.hasNext()) {
      AbstractInsnNode node = it.next();
      if (node instanceof LineNumberNode) {
        LineNumberNode lineNode = (LineNumberNode) node;
        lineNumber = lineNode.line;
      } else if (node instanceof MethodInsnNode) {
        MethodInsnNode callSite = (MethodInsnNode) node;
        switch (node.getOpcode()) {
          case Opcodes.INVOKEINTERFACE:
          case Opcodes.INVOKESPECIAL:
          case Opcodes.INVOKEVIRTUAL:
          case Opcodes.INVOKESTATIC:
            if (getCallSiteThrowableMutatorInstructions(
                cn,
                mn,
                callSite,
                lineNumber,
                localVariablesArgumentMap,
                throwableVarIndex,
                callSiteInstanceVarIndex,
                hooksToUse)) {
              ret = true;
            }
            if (getCallSiteStartInstructions(
                cn,
                mn,
                callSite,
                lineNumber,
                localVariablesArgumentMap,
                callSiteInstanceVarIndex,
                hooksToUse)) {
              ret = true;
            }
            if (Type.getReturnType(callSite.desc).getInternalName().equals("V")) {
              if (getCallSiteReturnVoidInstructions(
                  cn,
                  mn,
                  callSite,
                  lineNumber,
                  localVariablesArgumentMap,
                  callSiteInstanceVarIndex,
                  hooksToUse)) {
                ret = true;
              }
            } else {
              if (getCallSiteReturnMutatorInstructions(
                  cn,
                  mn,
                  callSite,
                  lineNumber,
                  localVariablesArgumentMap,
                  returnVariablesMap,
                  callSiteInstanceVarIndex,
                  hooksToUse)) {
                ret = true;
              }
            }
            // switch break
            break;
        }
      }
    }
    return ret;
  }

  private boolean getCallSiteStartInstructions(ClassNode cn, MethodNode mn, MethodInsnNode callSite,
      int lineNumber, int[][] localVariablesArgumentMap, int callSiteInstanceVarIndex,
      ArrayList<Integer> hooksToUse) {

    Type[] argTypes = Type.getArgumentTypes(callSite.desc);
    boolean staticCall = callSite.getOpcode() == Opcodes.INVOKESTATIC;
    InsnList il = null;
    for (int h = 0; h < hooksToUse.size(); h++) {
      Integer i = hooksToUse.get(h);
      int[] listenerArgs = localVariablesArgumentMap[i];
      if (listenerArgs != null) {
        CallSiteFilter filter = (CallSiteFilter) bctrace.getHooks()[i].getFilter();
        Object listener = bctrace.getHooks()[i].getListener();
        if (filter.acceptCallSite(cn, mn, callSite, lineNumber)) {
          if (il == null) {
            il = new InsnList();
          }
          // store local variables both for all DirectCallListeners
          il.add(storeCallSiteInstanceAndArgsInVariables(mn, argTypes, staticCall,
              localVariablesArgumentMap[i],
              callSiteInstanceVarIndex));

          if (listener instanceof DirectCallSiteStartListener) {
            DirectCallSiteStartListener directCallSiteStartListener = (DirectCallSiteStartListener) listener;

            // caller class, caller instance, callee instance
            // onBeforeCall(Class.class, Object.class, Object.class);
            il.add(ASMUtils.getPushInstruction(i)); // hook id
            il.add(
                getClassConstantReference(Type.getObjectType(cn.name), cn.version)); // caller class
            pushInstance(il, mn); // current instance
            if (callSite.getOpcode() == Opcodes.INVOKESTATIC) { // callee instance
              il.add(new InsnNode(Opcodes.ACONST_NULL));
            } else {
              il.add(new VarInsnNode(Opcodes.ALOAD, callSiteInstanceVarIndex));
            }
            // Move local variables to stack
            for (int j = 0; j < argTypes.length; j++) {
              Type argType = argTypes[j];
              il.add(ASMUtils.getLoadInst(argType, localVariablesArgumentMap[i][j]));
            }
            // Invoke dynamically generated callback method. See CallbackTransformer
            il.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                "io/shiftleft/bctrace/runtime/Callback",
                CallbackTransformer
                    .getDynamicListenerMethodName(directCallSiteStartListener),
                CallbackTransformer
                    .getDynamicListenerVoidMethodDescriptor(directCallSiteStartListener),
                false));
          }
        }
      }
    }
    if (il != null) {
      mn.instructions.insertBefore(callSite, il);
      return true;
    }
    return false;
  }

  private boolean getCallSiteReturnVoidInstructions(ClassNode cn, MethodNode mn,
      MethodInsnNode callSite, int lineNumber, int[][] localVariablesArgumentMap,
      int callSiteInstanceVarIndex,
      ArrayList<Integer> hooksToUse) {

    Type[] argTypes = Type.getArgumentTypes(callSite.desc);
    Type returnType = Type.getReturnType(callSite.desc);
    InsnList il = null;
    for (int h = hooksToUse.size() - 1; h >= 0; h--) {
      Integer i = hooksToUse.get(h);
      int[] listenerArgs = localVariablesArgumentMap[i];
      if (listenerArgs != null) {
        Object listener = bctrace.getHooks()[i].getListener();
        if (!(listener instanceof DirectCallSiteReturnListener)) {
          continue;
        }
        CallSiteFilter filter = (CallSiteFilter) bctrace.getHooks()[i].getFilter();
        if (filter.acceptCallSite(cn, mn, callSite, lineNumber)) {
          if (il == null) {
            il = new InsnList();
          }
          // caller class, caller instance, callee instance
          // onAfterCall(Class.class, Object.class, Object.class);
          il.add(ASMUtils.getPushInstruction(i)); // hook id
          il.add(
              getClassConstantReference(Type.getObjectType(cn.name), cn.version)); // caller class
          pushInstance(il, mn); // current instance
          if (callSite.getOpcode() == Opcodes.INVOKESTATIC) { // callee instance
            il.add(new InsnNode(Opcodes.ACONST_NULL));
          } else {
            il.add(new VarInsnNode(Opcodes.ALOAD, callSiteInstanceVarIndex));
          }
          // Move local variables to stack
          for (int j = 0; j < argTypes.length; j++) {
            Type argType = argTypes[j];
            il.add(ASMUtils.getLoadInst(argType, localVariablesArgumentMap[i][j]));
          }
          // Invoke dynamically generated callback method. See CallbackTransformer
          il.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
              "io/shiftleft/bctrace/runtime/Callback",
              CallbackTransformer.getDynamicListenerMethodName((DirectListener) listener),
              CallbackTransformer.getDynamicListenerVoidMethodDescriptor((DirectListener) listener),
              false));
        }
      }
    }
    if (il != null) {
      mn.instructions.insert(callSite, il);
      return true;
    }
    return false;
  }

  private boolean getCallSiteReturnMutatorInstructions(ClassNode cn, MethodNode mn,
      MethodInsnNode callSite, int lineNumber, int[][] localVariablesArgumentMap,
      int[] returnVariablesMap,
      int callSiteInstanceVarIndex,
      ArrayList<Integer> hooksToUse) {

    Type[] argTypes = Type.getArgumentTypes(callSite.desc);
    Type returnType = Type.getReturnType(callSite.desc);
    InsnList il = null;
    for (int h = hooksToUse.size() - 1; h >= 0; h--) {
      Integer i = hooksToUse.get(h);
      int[] listenerArgs = localVariablesArgumentMap[i];
      if (listenerArgs != null) {
        Object listener = bctrace.getHooks()[i].getListener();
        if (!(listener instanceof DirectCallSiteReturnListener)) {
          continue;
        }
        CallSiteFilter filter = (CallSiteFilter) bctrace.getHooks()[i].getFilter();
        if (filter.acceptCallSite(cn, mn, callSite, lineNumber)) {
          if (il == null) {
            il = new InsnList();
          }
          il.add(ASMUtils.getStoreInst(returnType, returnVariablesMap[i]));

          // caller class, caller instance, callee instance
          // onAfterCall(Class.class, Object.class, Object.class);

          il.add(ASMUtils.getPushInstruction(i)); // hook id
          il.add(ASMUtils.getLoadInst(returnType,
              returnVariablesMap[i]));  // original value consumed from the stack
          il.add(
              getClassConstantReference(Type.getObjectType(cn.name), cn.version)); // caller class
          pushInstance(il, mn); // current instance
          if (callSite.getOpcode() == Opcodes.INVOKESTATIC) { // callee instance
            il.add(new InsnNode(Opcodes.ACONST_NULL));
          } else {
            il.add(new VarInsnNode(Opcodes.ALOAD, callSiteInstanceVarIndex));
          }
          // Move local variables to stack
          for (int j = 0; j < argTypes.length; j++) {
            Type argType = argTypes[j];
            il.add(ASMUtils.getLoadInst(argType, localVariablesArgumentMap[i][j]));
          }
          // Move return value variables to stack
          if (!returnType.getDescriptor().equals("V")) {
            il.add(ASMUtils.getLoadInst(returnType, returnVariablesMap[i]));
          }
          // Invoke dynamically generated callback method. See CallbackTransformer
          il.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
              "io/shiftleft/bctrace/runtime/Callback",
              CallbackTransformer.getDynamicListenerMethodName((DirectListener) listener),
              CallbackTransformer
                  .getDynamicListenerMutatorMethodDescriptor((DirectListener) listener),
              false));
          // Update return value local variable, so each listener receives the modified value from the ones before
          // instead of getting all of them the original value
          il.add(ASMUtils.getStoreInst(returnType, returnVariablesMap[i]));
        }
      }
      if (il != null) {
        il.add(ASMUtils.getLoadInst(returnType, returnVariablesMap[i]));
      }
    }
    if (il != null) {
      mn.instructions.insert(callSite, il);
      return true;
    }
    return false;
  }


  private boolean getCallSiteThrowableMutatorInstructions(ClassNode cn, MethodNode mn,
      MethodInsnNode callSite, int lineNumber, int[][] localVariablesArgumentMap,
      int throwableVarIndex,
      int callSiteInstanceVarIndex, ArrayList<Integer> hooksToUse) {

    Type[] argTypes = Type.getArgumentTypes(callSite.desc);
    LabelNode handlerNode = null;
    InsnList il = null;
    for (int h = hooksToUse.size() - 1; h >= 0; h--) {
      Integer i = hooksToUse.get(h);
      int[] listenerArgs = localVariablesArgumentMap[i];
      if (listenerArgs != null) {
        Object listener = bctrace.getHooks()[i].getListener();
        if (!(listener instanceof DirectCallSiteThrowableListener)) {
          continue;
        }
        CallSiteFilter filter = (CallSiteFilter) bctrace.getHooks()[i].getFilter();
        if (filter.acceptCallSite(cn, mn, callSite, lineNumber)) {
          if (il == null) {
            il = new InsnList();
            Object[] parametersFrameTypes = ASMUtils.getParametersFrameTypes(cn, mn);
            il.add(
                new FrameNode(Opcodes.F_FULL, parametersFrameTypes.length, parametersFrameTypes, 1,
                    new Object[]{"java/lang/Throwable"}));
            handlerNode = new LabelNode();
            il.insert(handlerNode);
            il.add(new VarInsnNode(Opcodes.ASTORE, throwableVarIndex));
          }

          // caller class, caller instance, callee instance
          // onAfterCall(Class.class, Object.class, Object.class);

          il.add(ASMUtils.getPushInstruction(i)); // hook id
          il.add(new VarInsnNode(Opcodes.ALOAD,
              throwableVarIndex)); // original value consumed from the stack
          il.add(
              getClassConstantReference(Type.getObjectType(cn.name), cn.version)); // caller class
          pushInstance(il, mn); // current instance
          if (callSite.getOpcode() == Opcodes.INVOKESTATIC) { // callee instance
            il.add(new InsnNode(Opcodes.ACONST_NULL));
          } else {
            il.add(new VarInsnNode(Opcodes.ALOAD, callSiteInstanceVarIndex));
          }
          // Move local variables to stack
          for (int j = 0; j < argTypes.length; j++) {
            Type argType = argTypes[j];
            il.add(ASMUtils.getLoadInst(argType, localVariablesArgumentMap[i][j]));
          }
          // Move return value variables to stack
          il.add(new VarInsnNode(Opcodes.ALOAD, throwableVarIndex));
          // Invoke dynamically generated callback method. See CallbackTransformer
          il.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
              "io/shiftleft/bctrace/runtime/Callback",
              CallbackTransformer.getDynamicListenerMethodName((DirectListener) listener),
              CallbackTransformer
                  .getDynamicListenerMutatorMethodDescriptor((DirectListener) listener),
              false));
          // Update return value local variable, so each listener receives the modified value from the ones before
          // instead of getting all of them the original value
          il.add(new VarInsnNode(Opcodes.ASTORE, throwableVarIndex));
        }
      }
    }
    if (il != null) {
      LabelNode startNode = getStartNodeForTryCatch(mn, callSite);
      if (startNode != null) {
        il.add(new VarInsnNode(Opcodes.ALOAD, throwableVarIndex));
        il.add(new InsnNode(Opcodes.ATHROW));
        LabelNode endNode = new LabelNode();
        mn.instructions.insert(callSite, endNode);
        mn.instructions.add(il);
        TryCatchBlockNode blockNode = new TryCatchBlockNode(startNode, endNode, handlerNode, null);
        mn.tryCatchBlocks.add(blockNode);
        return true;
      }
    }
    return false;
  }

  /**
   * Returns a int[i] holding the indexes for the local variables created for holding return value
   * of the i-th listener. Updates maxlocals accordingly.
   */
  private int[] getReturnVariablesArgumentMap(ClassNode cn, MethodNode mn,
      ArrayList<Integer> hooksToUse) {
    Hook[] hooks = bctrace.getHooks();
    int[] map = new int[hooks.length];
    for (int h = 0; h < hooksToUse.size(); h++) {
      Integer i = hooksToUse.get(h);
      if (!(hooks[i].getFilter() instanceof CallSiteFilter)) {
        continue;
      }
      Object listener = bctrace.getHooks()[i].getListener();
      if (!(listener instanceof DirectCallSiteReturnListener)) {
        continue;
      }
      CallSiteFilter filter = (CallSiteFilter) hooks[i].getFilter();
      Iterator<AbstractInsnNode> it = mn.instructions.iterator();
      // We only want to reserve local variables for those listeners that apply to this method:
      int lineNumber = -1;
      while (it.hasNext()) {
        AbstractInsnNode node = it.next();
        if (node instanceof LineNumberNode) {
          LineNumberNode lineNode = (LineNumberNode) node;
          lineNumber = lineNode.line;
        } else if (node instanceof MethodInsnNode) {
          MethodInsnNode callSite = (MethodInsnNode) node;
          switch (node.getOpcode()) {
            case Opcodes.INVOKEINTERFACE:
            case Opcodes.INVOKESPECIAL:
            case Opcodes.INVOKEVIRTUAL:
            case Opcodes.INVOKESTATIC:
              if (filter.acceptCallSite(cn, mn, callSite, lineNumber)) {
                // If here, then this listener will be applied
                Type returnType = Type.getReturnType(callSite.desc);
                if (!returnType.getDescriptor().equals("V")) {
                  map[i] = mn.maxLocals;
                  mn.maxLocals = mn.maxLocals + returnType.getSize();
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
   * Returns a int[i][j] holding the indexes for the local variables created for holding the j-th
   * argument of the i-th listener. Updates maxlocals accordingly.
   */
  private int[][] getLocalVariablesArgumentMap(ClassNode cn, MethodNode mn,
      ArrayList<Integer> hooksToUse) {
    Hook[] hooks = bctrace.getHooks();
    int[][] map = new int[hooks.length][];
    for (int h = 0; h < hooksToUse.size(); h++) {
      Integer i = hooksToUse.get(h);
      if (!(hooks[i].getFilter() instanceof CallSiteFilter)) {
        continue;
      }
      CallSiteFilter filter = (CallSiteFilter) hooks[i].getFilter();
      Iterator<AbstractInsnNode> it = mn.instructions.iterator();
      // We only want to reserve local variables for those listeners that apply to this method
      int lineNumber = -1;
      while (it.hasNext()) {
        AbstractInsnNode node = it.next();
        if (node instanceof LineNumberNode) {
          LineNumberNode lineNode = (LineNumberNode) node;
          lineNumber = lineNode.line;
        } else if (node instanceof MethodInsnNode) {
          MethodInsnNode callSite = (MethodInsnNode) node;
          switch (node.getOpcode()) {
            case Opcodes.INVOKEINTERFACE:
            case Opcodes.INVOKESPECIAL:
            case Opcodes.INVOKEVIRTUAL:
            case Opcodes.INVOKESTATIC:
              if (filter.acceptCallSite(cn, mn, callSite, lineNumber)) {
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
  private InsnList storeCallSiteInstanceAndArgsInVariables(MethodNode mn, Type[] argTypes,
      boolean staticCall,
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
