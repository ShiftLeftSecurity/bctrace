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
import io.shiftleft.bctrace.hook.Hook;
import io.shiftleft.bctrace.runtime.listener.specific.DirectListener;
import io.shiftleft.bctrace.runtime.listener.specific.DirectListener.ListenerType;
import java.util.ArrayList;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

/**
 * @author Ignacio del Valle Alles idelvall@shiftleft.io
 */
public class Helper {

  protected Bctrace bctrace;

  public void setBctrace(Bctrace bctrace) {
    this.bctrace = bctrace;
  }

  protected boolean isInstrumentationNeeded(ArrayList<Integer> listenersToUse) {
    if (listenersToUse == null) {
      return false;
    }
    return listenersToUse.size() > 0;
  }

  protected ArrayList<Integer> getListenersOfType(ArrayList<Integer> hooksToUse,
      Class<?> clazz) {
    ArrayList<Integer> ret = null;
    Hook[] hooks = bctrace.getHooks();
    for (int h = 0; h < hooks.length; h++) {
      Integer i = hooksToUse.get(h);
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

  protected ArrayList<Integer> getDirectListenersOfType(ArrayList<Integer> hooksToUse,
      ListenerType type) {
    ArrayList<Integer> ret = null;
    Hook[] hooks = bctrace.getHooks();
    for (int h = 0; h < hooks.length; h++) {
      Integer i = hooksToUse.get(h);
      Object listener = hooks[i].getListener();
      if (listener instanceof DirectListener) {
        DirectListener directListener = (DirectListener) listener;
        if (directListener.getType() == type) {
          if (ret == null) {
            ret = new ArrayList<Integer>(hooksToUse.size());
          }
          ret.add(i);
        }
      }
    }
    return ret;
  }

  protected ArrayList<Integer> getHooksOfType(ArrayList<Integer> hooksToUse,
      Class<? extends Hook> clazz) {
    ArrayList<Integer> ret = null;
    for (Integer i : hooksToUse) {
      Hook[] hooks = bctrace.getHooks();
      if (hooks[i] != null && clazz.isAssignableFrom(hooks[i].getClass())) {
        if (ret == null) {
          ret = new ArrayList<Integer>(hooksToUse.size());
        }
        ret.add(i);
      }
    }
    return ret;
  }

  public InsnList getClassConstantReference(Type type, int version) {
    InsnList il = new InsnList();
    /*
     * The class version. The minor version is stored in the 16 most significant bits, and the major
     * version in the 16 least significant bits.
     */
    short majorVersion = (short) version;
    /*
     * A change in the way of referencing class literals was introduced in version 1.5
     */
    if (majorVersion >= Opcodes.V1_5) {
      il.add(new LdcInsnNode(type));

    } else {
      String fullyQualifiedName = type.getInternalName().replaceAll("/", ".");
      il.add(new LdcInsnNode(fullyQualifiedName));
      il.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
          "java/lang/Class", "forName",
          "(Ljava/lang/String;)Ljava/lang/Class;", false));
    }
    return il;
  }

  /**
   * Pushes the parameter object array reference on top of the operand stack
   */
  protected void pushMethodArgsArray(InsnList il, MethodNode mn) {
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

  /**
   * Appends to the list bytecodes to push the parameter arguments on top of the operand stack
   */
  protected void pushMethodArgs(InsnList il, MethodNode mn) {
    Type[] methodArguments = Type.getArgumentTypes(mn.desc);
    if (methodArguments.length > 0) {
      int index = ASMUtils.isStatic(mn.access) ? 0 : 1;
      for (int i = 0; i < methodArguments.length; i++) {
        il.add(ASMUtils.getLoadInst(methodArguments[i], index));
        index += methodArguments[i].getSize();
      }
    }
  }

  /**
   * Appends to the list bytecodes to push the instance on top of the operand stack, or null if the
   * method is static
   */
  protected void pushInstance(InsnList il, MethodNode mn) {
    if (ASMUtils.isStatic(mn.access) || mn.name.equals("<init>")) { // current instance
      il.add(new InsnNode(Opcodes.ACONST_NULL));
    } else {
      il.add(new VarInsnNode(Opcodes.ALOAD, 0));
    }
  }
}
