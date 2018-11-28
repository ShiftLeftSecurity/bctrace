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
package io.shiftleft.bctrace.asm.helper.generic;

import io.shiftleft.bctrace.Bctrace;
import io.shiftleft.bctrace.asm.helper.Helper;
import io.shiftleft.bctrace.asm.util.ASMUtils;
import io.shiftleft.bctrace.logging.Level;
import io.shiftleft.bctrace.runtime.listener.generic.FinishListener;
import java.util.ArrayList;
import java.util.Iterator;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LookupSwitchInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TableSwitchInsnNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import org.objectweb.asm.tree.TypeInsnNode;
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
public class FinishHelper extends Helper {


  public boolean addByteCodeInstructions(int methodId, ClassNode cn, MethodNode mn,
      ArrayList<Integer> hooksToUse) {

    ArrayList<Integer> listenersToUse = getListenersOfType(hooksToUse,
        FinishListener.class);

    if (!isInstrumentationNeeded(listenersToUse)) {
      return false;
    }

    addReturnTrace(methodId, cn, mn, listenersToUse);
    addTryCatchInstructions(methodId, cn, mn, listenersToUse);
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

  private InsnList getVoidReturnTraceInstructions(int methodId, ClassNode cn, MethodNode mn,
      ArrayList<Integer> listenersToUse) {
    InsnList il = new InsnList();
    for (int i = listenersToUse.size() - 1; i >= 0; i--) {
      Integer index = listenersToUse.get(i);
      FinishListener listener = (FinishListener) bctrace.getHooks()[index].getListener();
      il.add(new InsnNode(Opcodes.ACONST_NULL)); // return value
      il.add(new InsnNode(Opcodes.ACONST_NULL)); // throwable value
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
      if (listener.requiresArguments()) {
        pushMethodArgsArray(il, mn);
      } else {
        il.add(new InsnNode(Opcodes.ACONST_NULL));
      }
      il.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
          "io/shiftleft/bctrace/runtime/Callback", "onFinish",
          "(Ljava/lang/Object;Ljava/lang/Throwable;ILjava/lang/Class;Ljava/lang/Object;I[Ljava/lang/Object;)Ljava/lang/Object;",
          false));
      il.add(new InsnNode(Opcodes.POP));
    }
    return il;
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
      FinishListener listener = (FinishListener) bctrace.getHooks()[index].getListener();
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
      il.add(new InsnNode(Opcodes.DUP));
      il.add(new InsnNode(Opcodes.ACONST_NULL)); // throwable
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
          "io/shiftleft/bctrace/runtime/Callback", "onFinish",
          "(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Throwable;ILjava/lang/Class;Ljava/lang/Object;I[Ljava/lang/Object;)Ljava/lang/Object;",
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

  private boolean addTryCatchInstructions(int methodId, ClassNode cn, MethodNode mn,
      ArrayList<Integer> listenersToUse) {

    LabelNode startNode = getStartNodeForGlobalTryCatch(mn);
    if (startNode == null) {
      // Weird constructor, not generated from Java Language
      Bctrace.getAgentLogger().log(Level.WARNING,
          "Could not add try/catch handler to constructor " + cn.name + "." + mn.name + mn.desc);
      return false;
    }

    LabelNode endNode = new LabelNode();
    mn.instructions.add(endNode);

    InsnList il = new InsnList();
    Object[] parametersFrameTypes = ASMUtils.getParametersFrameTypes(cn, mn);
    il.add(new FrameNode(Opcodes.F_FULL, parametersFrameTypes.length, parametersFrameTypes, 1,
        new Object[]{"java/lang/Throwable"}));

    LabelNode handlerNode = new LabelNode();
    il.add(handlerNode);

    int thVarIndex = mn.maxLocals;
    mn.maxLocals = mn.maxLocals + 1;
    il.add(new VarInsnNode(Opcodes.ASTORE, thVarIndex));

    for (int i = 0; i < listenersToUse.size(); i++) {
      Integer index = listenersToUse.get(i);
      FinishListener listener = (FinishListener) bctrace.getHooks()[index]
          .getListener();
      il.add(new VarInsnNode(Opcodes.ALOAD, thVarIndex));
      il.add(new InsnNode(Opcodes.ACONST_NULL)); // return value
      il.add(new VarInsnNode(Opcodes.ALOAD, thVarIndex));
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
          "io/shiftleft/bctrace/runtime/Callback", "onFinish",
          "(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Throwable;ILjava/lang/Class;Ljava/lang/Object;I[Ljava/lang/Object;)Ljava/lang/Object;",
          false));

      il.add(new TypeInsnNode(Opcodes.CHECKCAST, "java/lang/Throwable"));
    }

    il.add(new InsnNode(Opcodes.ATHROW));

    TryCatchBlockNode blockNode = new TryCatchBlockNode(startNode, endNode, handlerNode, null);

    mn.tryCatchBlocks.add(blockNode);
    mn.instructions.add(il);
    return true;
  }
}
