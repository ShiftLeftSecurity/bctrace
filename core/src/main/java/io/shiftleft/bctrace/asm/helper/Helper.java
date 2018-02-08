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
import io.shiftleft.bctrace.runtime.listener.Listener;
import io.shiftleft.bctrace.spi.Hook;
import java.util.ArrayList;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;

/**
 * @author Ignacio del Valle Alles idelvall@shiftleft.io
 */
public class Helper {

  protected static boolean isInstrumentationNeeded(ArrayList<Integer> listenersToUse) {
    if (listenersToUse == null) {
      return false;
    }
    return listenersToUse.size() > 0;
  }

  protected static ArrayList<Integer> getListenersOfType(ArrayList<Integer> hooksToUse,
      Class<? extends Listener> clazz) {
    ArrayList<Integer> ret = null;
    for (Integer i : hooksToUse) {
      Hook[] hooks = Bctrace.getInstance().getHooks();
      if (hooks[i].getListener() != null &&
          clazz.isAssignableFrom(hooks[i].getListener().getClass())) {
        if (ret == null) {
          ret = new ArrayList<Integer>(hooksToUse.size());
        }
        ret.add(i);
      }
    }
    return ret;
  }

  /**
   * Pushes the parameter object array reference on top of the operand stack
   */
  protected static void pushMethodArgsArray(InsnList il, MethodNode mn) {
    Type[] methodArguments = Type.getArgumentTypes(mn.desc);
    if (methodArguments.length == 0) {
      il.add(new InsnNode(Opcodes.ACONST_NULL));
    } else {
      il.add(ASMUtils.getPushInstruction(methodArguments.length));
      il.add(new TypeInsnNode(Opcodes.ANEWARRAY, "java/lang/Object"));
      int index = ASMUtils.isStatic(mn.access) ? 0 : 1;
      for (int i = 0; i < methodArguments.length; i++) {
        il.add(new InsnNode(Opcodes.DUP));
        il.add(ASMUtils.getPushInstruction(i));
        il.add(ASMUtils.getLoadInst(methodArguments[i], index));
        MethodInsnNode primitiveToWrapperInst = ASMUtils
            .getPrimitiveToWrapperInst(methodArguments[i]);
        if (primitiveToWrapperInst != null) {
          il.add(primitiveToWrapperInst);
        }
        il.add(new InsnNode(Opcodes.AASTORE));
        index += methodArguments[i].getSize();
      }
    }
  }

}
