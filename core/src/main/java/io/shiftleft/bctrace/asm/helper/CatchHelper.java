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
import io.shiftleft.bctrace.runtime.listener.info.FinishThrowableListener;
import java.util.ArrayList;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import org.objectweb.asm.tree.VarInsnNode;

/**
 * Inserts the bytecode instructions within method node, needed to communicate the throwables rised
 * by the target method at runtime to the listeners registered.
 *
 * This helper turns the method node instructions of a method like this:
 * <br><pre>{@code
 * public Object foo(Object args){
 *   return void(args);
 * }
 * }
 * </pre>
 * Into this:
 * <br><pre>{@code
 *public Object foo(Object args){
 *   try{
 *     return void(args);
 *   } catch (Throwable th){
 *     // Notify listeners that apply to this method
 *     Callback.onFinishedThrowable(th, this, 0);
 *     Callback.onFinishedThrowable(th, this, 2);
 *     Callback.onFinishedThrowable(th, this, 10);
 *     // Throw the cathed instance
 *     throw th:
 *   }
 * }
 * }
 * </pre>
 * @author Ignacio del Valle Alles idelvall@shiftleft.io
 */
public class CatchHelper extends Helper {

  public static void addByteCodeInstructions(int methodId, ClassNode cn, MethodNode mn,
      ArrayList<Integer> hooksToUse) {

    ArrayList<Integer> listenersToUse = getListenersOfType(hooksToUse,
        FinishThrowableListener.class);

    LabelNode startNode = insertStartNode(mn, listenersToUse);
    addTraceThrowableUncaught(methodId, mn, startNode, listenersToUse);
  }

  private static LabelNode insertStartNode(MethodNode mn, ArrayList<Integer> listenersToUse) {
    if (!isInstrumentationNeeded(listenersToUse)) {
      return null;
    }
    LabelNode ret = new LabelNode();
    mn.instructions.insert(ret);
    return ret;
  }

  private static void addTraceThrowableUncaught(int methodId, MethodNode mn, LabelNode startNode,
      ArrayList<Integer> listenersToUse) {
    if (!isInstrumentationNeeded(listenersToUse)) {
      return;
    }

    InsnList il = mn.instructions;

    LabelNode endNode = new LabelNode();
    il.add(endNode);

    addCatchBlock(methodId, mn, startNode, endNode, listenersToUse);
  }

  private static void addCatchBlock(int methodId, MethodNode mn, LabelNode startNode,
      LabelNode endNode, ArrayList<Integer> listenersToUse) {

    InsnList il = new InsnList();
    LabelNode handlerNode = new LabelNode();
    il.add(handlerNode);
    il.add(getThrowableTraceInstructions(methodId, mn, listenersToUse));
    il.add(new InsnNode(Opcodes.ATHROW));

    TryCatchBlockNode blockNode = new TryCatchBlockNode(startNode, endNode, handlerNode,
        "java/lang/Throwable");
    mn.tryCatchBlocks.add(blockNode);
    mn.instructions.add(il);
  }

  private static InsnList getThrowableTraceInstructions(int methodId, MethodNode mn,
      ArrayList<Integer> listenersToUse) {
    InsnList il = new InsnList();
    for (int i = listenersToUse.size() - 1; i >= 0; i--) {
      Integer index = listenersToUse.get(i);
      il.add(new InsnNode(Opcodes.DUP)); // dup throwable
      il.add(ASMUtils.getPushInstruction(methodId)); // method id
      if (ASMUtils.isStatic(mn.access) || mn.name.equals("<init>")) { // instance
        il.add(new InsnNode(Opcodes.ACONST_NULL));
      } else {
        il.add(new VarInsnNode(Opcodes.ALOAD, 0));
      }
      il.add(ASMUtils.getPushInstruction(index)); // hook id
      il.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
          "io/shiftleft/bctrace/runtime/Callback", "onFinishedThrowable",
          "(Ljava/lang/Throwable;ILjava/lang/Object;I)V", false));
    }
    return il;
  }
}
