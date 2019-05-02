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
package io.shiftleft.bctrace.asm.primitive;

import io.shiftleft.bctrace.Bctrace;
import io.shiftleft.bctrace.MethodRegistry;
import io.shiftleft.bctrace.MethodRegistryImpl;
import io.shiftleft.bctrace.asm.util.ASMUtils;
import io.shiftleft.bctrace.hook.Hook;
import java.util.ArrayList;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.LookupSwitchInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TableSwitchInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

/**
 * @author Ignacio del Valle Alles idelvall@shiftleft.io
 */
public abstract class InstrumentationPrimitive {

  protected Bctrace bctrace;

  public abstract boolean addByteCodeInstructions(int methodId, ClassNode cn, MethodNode mn,
      ArrayList<Integer> hooksToUse);

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
    for (int h = 0; h < hooksToUse.size(); h++) {
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

  protected ArrayList<Integer> getHooksOfType(ArrayList<Integer> hooksToUse,
      Class<? extends Hook> clazz) {
    ArrayList<Integer> ret = null;
    for (int h = 0; h < hooksToUse.size(); h++) {
      Integer i = hooksToUse.get(h);
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
   * Appends to the list byte codes to push the instance on top of the operand stack, or null if the
   * method is static
   */
  protected void pushInstance(InsnList il, MethodNode mn) {
    pushInstance(il, mn, false);
  }

  /**
   * Appends to the list bytecodes to push the instance on top of the operand stack, or null if the
   * method is static
   */
  protected void pushInstance(InsnList il, MethodNode mn, boolean initialized) {
    if (ASMUtils.isStatic(mn.access)
        || mn.name.equals("<init>") && !initialized) { // current instance
      il.add(new InsnNode(Opcodes.ACONST_NULL));
    } else {
      il.add(new VarInsnNode(Opcodes.ALOAD, 0));
    }
  }

  protected LabelNode getStartNodeForGlobalTryCatch(MethodNode mn) {
    LabelNode startNode = new LabelNode();
    // Look for call to super constructor in the top frame (before any jump)
    if (mn.name.equals("<init>")) {
      InsnList il = mn.instructions;
      AbstractInsnNode superCall = null;
      AbstractInsnNode node = il.getFirst();
      int newCalls = 0;
      while (node != null) {
        if (node instanceof JumpInsnNode ||
            node instanceof TableSwitchInsnNode ||
            node instanceof LookupSwitchInsnNode) {
          // No branching supported before call to super()
          break;
        }
        if (node.getOpcode() == Opcodes.NEW) {
          newCalls++;
        }
        if (node instanceof MethodInsnNode && node.getOpcode() == Opcodes.INVOKESPECIAL) {
          MethodInsnNode min = (MethodInsnNode) node;
          if (min.name.equals("<init>")) {
            if (newCalls == 0) {
              superCall = min;
              break;
            } else {
              newCalls--;
            }
          }
        }
        node = node.getNext();
      }
      if (superCall == null) {
        // Weird constructor, not generated from Java Language
        return null;
      } else {
        // If super() call is found, then add the try/catch after it, so the instance is initialized
        // https://docs.oracle.com/javase/specs/jvms/se7/html/jvms-4.html#jvms-4.10.1.9.invokespecial
        if (superCall.getNext() == null) {
          mn.instructions.add(startNode);
        } else {
          mn.instructions.insertBefore(superCall.getNext(), startNode);
        }
      }
    } else {
      mn.instructions.insert(startNode);
    }
    return startNode;
  }

  /**
   * Adds label node before the specified instruction and returns it.
   *
   * @param mn All method instructions
   * @param instruction The instruction to add into the try-catch
   * @return null if it can not be added (nodes in constructor before call to super constructor)
   */
  protected LabelNode getStartNodeForTryCatch(MethodNode mn, AbstractInsnNode instruction) {
    boolean inited;
    if (mn.name.equals("<init>")) {
      inited = false;
      InsnList il = mn.instructions;
      AbstractInsnNode superCall = null;
      AbstractInsnNode node = il.getFirst();
      int newCalls = 0;
      while (node != null) {
        if (node instanceof JumpInsnNode ||
            node instanceof TableSwitchInsnNode ||
            node instanceof LookupSwitchInsnNode) {
          // No branching supported before call to super()
          break;
        }
        if (node.getOpcode() == Opcodes.NEW) {
          newCalls++;
        }
        if (node instanceof MethodInsnNode && node.getOpcode() == Opcodes.INVOKESPECIAL) {
          MethodInsnNode min = (MethodInsnNode) node;
          if (min.name.equals("<init>")) {
            if (newCalls == 0) {
              inited = true;
              break;
            } else {
              newCalls--;
            }
          }
        }
        node = node.getNext();
      }
    } else {
      inited = true;
    }
    if (inited) {
      LabelNode startNode = new LabelNode();
      mn.instructions.insertBefore(instruction, startNode);
      return startNode;
    } else {
      return null;
    }
  }
}
