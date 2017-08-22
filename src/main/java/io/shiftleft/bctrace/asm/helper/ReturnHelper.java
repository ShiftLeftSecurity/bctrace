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

import java.util.Iterator;
import java.util.LinkedList;
import io.shiftleft.bctrace.asm.utils.ASMUtils;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

/**
 *
 * @author Ignacio del Valle Alles idelvall@shiftleft.io
 */
public class ReturnHelper {

  public static void addTraceReturn(MethodNode mn, int frameDataVarIndex, LinkedList<Integer> hooksToUse) {
    InsnList il = mn.instructions;
    Iterator<AbstractInsnNode> it = il.iterator();
    Type returnType = Type.getReturnType(mn.desc);

    while (it.hasNext()) {
      AbstractInsnNode abstractInsnNode = it.next();

      switch (abstractInsnNode.getOpcode()) {
        case Opcodes.RETURN:
          il.insertBefore(abstractInsnNode, getVoidReturnTraceInstructions(frameDataVarIndex, hooksToUse));
          break;
        case Opcodes.IRETURN:
        case Opcodes.LRETURN:
        case Opcodes.FRETURN:
        case Opcodes.ARETURN:
        case Opcodes.DRETURN:
          il.insertBefore(abstractInsnNode, getReturnTraceInstructions(returnType, frameDataVarIndex, hooksToUse));
      }
    }
  }

  private static InsnList getVoidReturnTraceInstructions(int frameDataVarIndex, LinkedList<Integer> hooksToUse) {
    InsnList il = new InsnList();
    Iterator<Integer> descendingIterator = hooksToUse.descendingIterator();
    while (descendingIterator.hasNext()) {
      Integer index = descendingIterator.next();
      il.add(new InsnNode(Opcodes.ACONST_NULL));
      il.add(new VarInsnNode(Opcodes.ALOAD, frameDataVarIndex));
      il.add(ASMUtils.getPushInstruction(index));
      il.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
              "io/shiftleft/bctrace/runtime/Callback", "onFinishedReturn",
              "(Ljava/lang/Object;Lio/shiftleft/bctrace/runtime/FrameData;I)V", false));
    }
    return il;
  }

  private static InsnList getReturnTraceInstructions(Type returnType, int frameDataVarIndex, LinkedList<Integer> hooksToUse) {
    InsnList il = new InsnList();
    Iterator<Integer> descendingIterator = hooksToUse.descendingIterator();
    while (descendingIterator.hasNext()) {
      Integer index = descendingIterator.next();
      if (returnType.getSize() == 1) {
        il.add(new InsnNode(Opcodes.DUP));
      } else {
        il.add(new InsnNode(Opcodes.DUP2));
      }
      MethodInsnNode mNode = ASMUtils.getWrapperContructionInst(returnType);
      if (mNode != null) {
        il.add(mNode);
      }
      il.add(new VarInsnNode(Opcodes.ALOAD, frameDataVarIndex));
      il.add(ASMUtils.getPushInstruction(index));
      il.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
              "io/shiftleft/bctrace/runtime/Callback", "onFinishedReturn",
              "(Ljava/lang/Object;Lio/shiftleft/bctrace/runtime/FrameData;I)V", false));
    }
    return il;
  }
}
