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
package io.shiftleft.bctrace.asm.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.objectweb.asm.util.Printer;
import org.objectweb.asm.util.Textifier;
import org.objectweb.asm.util.TraceMethodVisitor;

/**
 * @author Ignacio del Valle Alles idelvall@shiftleft.io
 */
public class ASMUtils {

  public static boolean isInterface(int modifiers) {
    return (modifiers & Opcodes.ACC_INTERFACE) != 0;
  }

  public static boolean isAbstract(int modifiers) {
    return (modifiers & Opcodes.ACC_ABSTRACT) != 0;
  }

  public static boolean isNative(int modifiers) {
    return (modifiers & Opcodes.ACC_NATIVE) != 0;
  }

  public static boolean isStatic(int modifiers) {
    return (modifiers & Opcodes.ACC_STATIC) != 0;
  }

  public static boolean isPublic(int modifiers) {
    return (modifiers & Opcodes.ACC_PUBLIC) != 0;
  }

  public static boolean isProtected(int modifiers) {
    return (modifiers & Opcodes.ACC_PROTECTED) != 0;
  }

  public static boolean isPrivate(int modifiers) {
    return (modifiers & Opcodes.ACC_PRIVATE) != 0;
  }

  public static VarInsnNode getLoadInst(Type type, int position) {
    int opCode = -1;
    switch (type.getDescriptor().charAt(0)) {
      case 'B':
        opCode = Opcodes.ILOAD;
        break;
      case 'C':
        opCode = Opcodes.ILOAD;
        break;
      case 'D':
        opCode = Opcodes.DLOAD;
        break;
      case 'F':
        opCode = Opcodes.FLOAD;
        break;
      case 'I':
        opCode = Opcodes.ILOAD;
        break;
      case 'J':
        opCode = Opcodes.LLOAD;
        break;
      case 'L':
        opCode = Opcodes.ALOAD;
        break;
      case '[':
        opCode = Opcodes.ALOAD;
        break;
      case 'Z':
        opCode = Opcodes.ILOAD;
        break;
      case 'S':
        opCode = Opcodes.ILOAD;
        break;
      default:
        throw new ClassFormatError("Invalid method signature: " + type.getDescriptor());
    }
    return new VarInsnNode(opCode, position);
  }

  public static InsnNode getReturnInst(Type type) {
    int opCode = -1;
    switch (type.getDescriptor().charAt(0)) {
      case 'B':
        opCode = Opcodes.IRETURN;
        break;
      case 'C':
        opCode = Opcodes.IRETURN;
        break;
      case 'D':
        opCode = Opcodes.DRETURN;
        break;
      case 'F':
        opCode = Opcodes.FRETURN;
        break;
      case 'I':
        opCode = Opcodes.IRETURN;
        break;
      case 'J':
        opCode = Opcodes.LRETURN;
        break;
      case 'L':
        opCode = Opcodes.ARETURN;
        break;
      case '[':
        opCode = Opcodes.ARETURN;
        break;
      case 'Z':
        opCode = Opcodes.IRETURN;
        break;
      case 'S':
        opCode = Opcodes.IRETURN;
        break;
      default:
        throw new ClassFormatError("Invalid return type: " + type.getDescriptor());
    }
    return new InsnNode(opCode);
  }

  public static String getWrapper(Type primitiveType) {

    if (primitiveType.getDescriptor().length() != 1) {
      return null;
    }
    char charType = primitiveType.getDescriptor().charAt(0);
    String wrapper;
    switch (charType) {
      case 'B':
        wrapper = "java/lang/Byte";
        break;
      case 'C':
        wrapper = "java/lang/Character";
        break;
      case 'D':
        wrapper = "java/lang/Double";
        break;
      case 'F':
        wrapper = "java/lang/Float";
        break;
      case 'I':
        wrapper = "java/lang/Integer";
        break;
      case 'J':
        wrapper = "java/lang/Long";
        break;
      case 'L':
        return null;
      case '[':
        return null;
      case 'Z':
        wrapper = "java/lang/Boolean";
        break;
      case 'S':
        wrapper = "java/lang/Short";
        break;
      default:
        throw new ClassFormatError("Invalid type descriptor: "
            + primitiveType.getDescriptor());
    }
    return wrapper;
  }

  public static String getPrimitiveMethodName(String wrapper) {

    if (wrapper == null) {
      return null;
    }
    if (wrapper.equals("java/lang/Byte")) {
      return "byteValue";
    }
    if (wrapper.equals("java/lang/Character")) {
      return "charValue";
    }
    if (wrapper.equals("java/lang/Double")) {
      return "doubleValue";
    }
    if (wrapper.equals("java/lang/Float")) {
      return "floatValue";
    }
    if (wrapper.equals("java/lang/Integer")) {
      return "intValue";
    }
    if (wrapper.equals("java/lang/Long")) {
      return "longValue";
    }
    if (wrapper.equals("java/lang/Boolean")) {
      return "booleanValue";
    }
    if (wrapper.equals("java/lang/Short")) {
      return "shortValue";
    }
    throw new ClassFormatError("Invalid wrapper type: " + wrapper);
  }

  public static MethodInsnNode getPrimitiveToWrapperInst(Type type) {

    String wrapper = getWrapper(type);
    if (wrapper == null) {
      return null;
    }
    return new MethodInsnNode(Opcodes.INVOKESTATIC, wrapper, "valueOf",
        "(" + type.getDescriptor() + ")L" + wrapper + ";", false);

  }

  public static MethodInsnNode getWrapperToPrimitiveInst(Type primitiveType) {

    String wrapper = getWrapper(primitiveType);
    if (wrapper == null) {
      return null;
    }
    return new MethodInsnNode(Opcodes.INVOKEVIRTUAL, wrapper, getPrimitiveMethodName(wrapper),
        "()" + primitiveType.getDescriptor(), false);

  }

  public static VarInsnNode getStoreInst(Type type, int position) {
    int opCode = -1;
    switch (type.getDescriptor().charAt(0)) {
      case 'B':
        opCode = Opcodes.ISTORE;
        break;
      case 'C':
        opCode = Opcodes.ISTORE;
        break;
      case 'D':
        opCode = Opcodes.DSTORE;
        break;
      case 'F':
        opCode = Opcodes.FSTORE;
        break;
      case 'I':
        opCode = Opcodes.ISTORE;
        break;
      case 'J':
        opCode = Opcodes.LSTORE;
        break;
      case 'L':
        opCode = Opcodes.ASTORE;
        break;
      case '[':
        opCode = Opcodes.ASTORE;
        break;
      case 'Z':
        opCode = Opcodes.ISTORE;
        break;
      case 'S':
        opCode = Opcodes.ISTORE;
        break;
      default:
        throw new ClassFormatError("Invalid method signature: "
            + type.getDescriptor());
    }
    return new VarInsnNode(opCode, position);
  }

  public static MethodNode cloneMethod(MethodNode mn) {
    MethodNode cloned = new MethodNode(Opcodes.ASM5, mn.access, mn.name, mn.desc, mn.signature,
        mn.exceptions == null ? new String[0]
            : mn.exceptions.toArray(new String[mn.exceptions.size()]));
    mn.accept(cloned);
//    cloned.name = mn.name;
//    cloned.access = mn.access;
//    cloned.instructions = mn.instructions;
//    cloned.desc = mn.desc;
//    cloned.signature = mn.signature;
//    cloned.annotationDefault = mn.annotationDefault;
//    cloned.attrs = mn.attrs;
//    cloned.exceptions = mn.exceptions;
//    cloned.invisibleAnnotations = mn.invisibleAnnotations;
//    cloned.invisibleParameterAnnotations = mn.invisibleParameterAnnotations;
//    cloned.invisibleLocalVariableAnnotations = mn.invisibleLocalVariableAnnotations;
//    cloned.invisibleTypeAnnotations = mn.invisibleTypeAnnotations;
//    cloned.localVariables = mn.localVariables;
//    cloned.maxLocals = mn.maxLocals;
//    cloned.maxStack = mn.maxStack;
//    cloned.parameters = mn.parameters;
//    cloned.tryCatchBlocks = mn.tryCatchBlocks;
//    cloned.visibleAnnotations = mn.visibleAnnotations;
//    cloned.visibleLocalVariableAnnotations = mn.visibleLocalVariableAnnotations;
//    cloned.visibleParameterAnnotations = mn.visibleParameterAnnotations;
    //   cloned.visibleTypeAnnotations = mn.visibleTypeAnnotations;

    return cloned;
  }

  public static AbstractInsnNode getPushInstruction(int value) {

    if (value == -1) {
      return new InsnNode(Opcodes.ICONST_M1);
    } else if (value == 0) {
      return new InsnNode(Opcodes.ICONST_0);
    } else if (value == 1) {
      return new InsnNode(Opcodes.ICONST_1);
    } else if (value == 2) {
      return new InsnNode(Opcodes.ICONST_2);
    } else if (value == 3) {
      return new InsnNode(Opcodes.ICONST_3);
    } else if (value == 4) {
      return new InsnNode(Opcodes.ICONST_4);
    } else if (value == 5) {
      return new InsnNode(Opcodes.ICONST_5);
    } else if ((value >= -128) && (value <= 127)) {
      return new IntInsnNode(Opcodes.BIPUSH, value);
    } else if ((value >= -32768) && (value <= 32767)) {
      return new IntInsnNode(Opcodes.SIPUSH, value);
    } else {
      return new LdcInsnNode(value);
    }
  }

  public static byte[] toByteArray(InputStream is) throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    byte[] buffer = new byte[1024];
    while (true) {
      int r = is.read(buffer);
      if (r == -1) {
        break;
      }
      out.write(buffer, 0, r);
    }
    return out.toByteArray();
  }

  public static void viewByteCode(byte[] bytecode) {
    ClassReader cr = new ClassReader(bytecode);
    ClassNode cn = new ClassNode();
    cr.accept(cn, 0);
    final List<MethodNode> mns = cn.methods;
    Printer printer = new Textifier();
    TraceMethodVisitor mp = new TraceMethodVisitor(printer);
    for (MethodNode mn : mns) {
      InsnList inList = mn.instructions;
      System.out.println(mn.name);
      for (int i = 0; i < inList.size(); i++) {
        inList.get(i).accept(mp);
        StringWriter sw = new StringWriter();
        printer.print(new PrintWriter(sw));
        printer.getText().clear();
        System.out.print(sw.toString());
      }
    }
  }


  public static void main(String[] args) {
    int mod = Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_PRIVATE;
    mod = mod & ~Opcodes.ACC_PRIVATE;
    System.err.println(isPublic(mod));
    System.err.println(isStatic(mod));
    System.err.println(isProtected(mod));
    System.err.println(isPrivate(mod));
  }
}
