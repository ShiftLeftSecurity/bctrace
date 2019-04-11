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
package io.shiftleft.bctrace.asm.helper.direct.method;

import io.shiftleft.bctrace.Bctrace;
import io.shiftleft.bctrace.asm.CallbackTransformer;
import io.shiftleft.bctrace.asm.helper.Helper;
import io.shiftleft.bctrace.asm.util.ASMUtils;
import io.shiftleft.bctrace.logging.Level;
import io.shiftleft.bctrace.runtime.listener.direct.DirectListener;
import io.shiftleft.bctrace.runtime.listener.direct.DirectMethodThrowableListener;
import java.util.ArrayList;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import org.objectweb.asm.tree.VarInsnNode;

public class DirectMethodThrowableHelper extends Helper {

  public boolean addByteCodeInstructions(ClassNode cn, MethodNode mn,
      ArrayList<Integer> hooksToUse) {

    ArrayList<Integer> listenersToUse = getListenersOfType(hooksToUse,
        DirectMethodThrowableListener.class);
    if (!isInstrumentationNeeded(listenersToUse)) {
      return false;
    }
    addTryCatchInstructions(cn, mn, listenersToUse);
    return true;
  }

  private boolean addTryCatchInstructions(ClassNode cn, MethodNode mn,
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
    Object[] topLocals = ASMUtils.getTopLocals(cn, mn);
    il.add(new FrameNode(Opcodes.F_FULL, topLocals.length, topLocals, 1,
        new Object[]{"java/lang/Throwable"}));

    LabelNode handlerNode = new LabelNode();
    il.add(handlerNode);

    int thVarIndex = mn.maxLocals;
    mn.maxLocals = mn.maxLocals + 1;
    il.add(new VarInsnNode(Opcodes.ASTORE, thVarIndex));

    for (int i = 0; i < listenersToUse.size(); i++) {
      Integer index = listenersToUse.get(i);
      DirectListener listener = (DirectListener) bctrace.getHooks()[index]
          .getListener();
      il.add(ASMUtils.getPushInstruction(index)); // hook id
      il.add(new VarInsnNode(Opcodes.ALOAD, thVarIndex));// original value consumed from the stack
      il.add(getClassConstantReference(Type.getObjectType(cn.name), cn.version)); // caller class
      pushInstance(il, mn); // current instance
      pushMethodArgs(il, mn); // method args
      il.add(new VarInsnNode(Opcodes.ALOAD, thVarIndex));
      // Invoke dynamically generated callback method. See CallbackTransformer
      il.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
          "io/shiftleft/bctrace/runtime/Callback",
          CallbackTransformer.getDynamicListenerMethodName(listener),
          CallbackTransformer.getDynamicListenerMutatorMethodDescriptor(listener),
          false));
      // Update return value local variable, so each listener receives the modified value from the ones before
      // instead of getting all of them the original value
      il.add(new VarInsnNode(Opcodes.ASTORE, thVarIndex));
    }
    il.add(new VarInsnNode(Opcodes.ALOAD, thVarIndex));
    il.add(new InsnNode(Opcodes.ATHROW));

    TryCatchBlockNode blockNode = new TryCatchBlockNode(startNode, endNode, handlerNode, null);

    mn.tryCatchBlocks.add(blockNode);
    mn.instructions.add(il);
    return true;
  }
}
