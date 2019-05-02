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
package io.shiftleft.bctrace.asm.primitive.direct.method;

import io.shiftleft.bctrace.asm.CallbackTransformer;
import io.shiftleft.bctrace.asm.primitive.InstrumentationPrimitive;
import io.shiftleft.bctrace.asm.util.ASMUtils;
import io.shiftleft.bctrace.hook.Hook;
import io.shiftleft.bctrace.runtime.listener.direct.DirectMethodStartListener;
import java.util.ArrayList;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

public class DirectMethodStartPrimitive extends InstrumentationPrimitive {

  @Override
  public boolean addByteCodeInstructions(int methodId, ClassNode cn, MethodNode mn,
      ArrayList<Integer> hooksToUse) {

    ArrayList<Integer> listenersToUse = getListenersOfType(hooksToUse,
        DirectMethodStartListener.class);
    if (!isInstrumentationNeeded(listenersToUse)) {
      return false;
    }
    addTraceStart(cn, mn, listenersToUse);
    return true;
  }

  private void addTraceStart(ClassNode cn, MethodNode mn,
      ArrayList<Integer> listenersToUse) {

    InsnList il = new InsnList();
    Hook[] hooks = bctrace.getHooks();
    int offset = ASMUtils.isStatic(mn.access) ? 0 : 1;
    for (int i = 0; i < listenersToUse.size(); i++) {
      Integer index = listenersToUse.get(i);
      DirectMethodStartListener listener = (DirectMethodStartListener) hooks[index].getListener();
      String mutableDesc = null;

      il.add(ASMUtils.getPushInstruction(index)); // hook id
      int mai = listener.getMutableArgumentIndex();
      if (mai >= 0) {
        mutableDesc = CallbackTransformer.getDynamicListenerMutatorMethodDescriptor(listener);
        il.add(ASMUtils.getLoadInst(Type.getReturnType(mutableDesc), offset + mai));
      }
      il.add(getClassConstantReference(Type.getObjectType(cn.name), cn.version)); // caller class
      pushInstance(il, mn); // current instance
      pushMethodArgs(il, mn); // method args
      if (mai >= 0) {
        il.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
            "io/shiftleft/bctrace/runtime/Callback",
            CallbackTransformer.getDynamicListenerMethodName(listener),
            mutableDesc,
            false));
        il.add(ASMUtils.getStoreInst(Type.getReturnType(mutableDesc), offset + mai));
      } else {
        il.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
            "io/shiftleft/bctrace/runtime/Callback",
            CallbackTransformer.getDynamicListenerMethodName(listener),
            CallbackTransformer.getDynamicListenerVoidMethodDescriptor(listener),
            false));
      }
    }
    mn.instructions.insert(il);
  }
}
