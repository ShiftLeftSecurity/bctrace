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
package io.shiftleft.bctrace.asm;

import io.shiftleft.bctrace.Bctrace;
import io.shiftleft.bctrace.asm.util.ASMUtils;
import io.shiftleft.bctrace.hook.Hook;
import io.shiftleft.bctrace.hook.direct.DirectHook;
import io.shiftleft.bctrace.logging.Level;
import io.shiftleft.bctrace.runtime.listener.direct.DirectListener;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.reflect.Method;
import java.security.ProtectionDomain;
import java.util.HashSet;
import java.util.Set;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

/**
 * Adds synthetic methods to Callback class to backup direct listeners.
 *
 * @author Ignacio del Valle Alles idelvall@shiftleft.io
 */
public class CallbackTransformer implements ClassFileTransformer {

  private static final String CALLBACK_JVM_CLASS_NAME = "io/shiftleft/bctrace/runtime/Callback";
  private final Hook[] hooks;
  private volatile boolean completed = false;

  public CallbackTransformer(Hook[] hooks) {
    this.hooks = hooks;
  }

  @Override
  public byte[] transform(final ClassLoader loader,
      final String className,
      final Class<?> classBeingRedefined,
      final ProtectionDomain protectionDomain,
      final byte[] classfileBuffer)
      throws IllegalClassFormatException {

    if (className == null || !className.equals(CALLBACK_JVM_CLASS_NAME)) {
      return null;
    }

    try {
      Set<DirectListener> dynamicListeners = new HashSet<DirectListener>();
      for (int i = 0; i < this.hooks.length; i++) {
        if (this.hooks[i] instanceof DirectHook) {
          DirectHook dynamicHook = (DirectHook) this.hooks[i];
          dynamicListeners.add(dynamicHook.getListener());
        }
      }
      if (dynamicListeners.size() == 0) {
        this.completed = true;
        return null;
      }
      ClassReader cr = new ClassReader(
          ClassLoader.getSystemResourceAsStream(CALLBACK_JVM_CLASS_NAME + ".class"));
      ClassNode cn = new ClassNode();
      cn.version = Opcodes.V1_6;
      cr.accept(cn, 0);
      HashSet<String> methodsAdded = new HashSet<String>();
      for (DirectListener dynamicListener : dynamicListeners) {
        String key = getDynamicListenerMethodName(dynamicListener);
        if (methodsAdded.contains(key)) {
          continue;
        }
        methodsAdded.add(key);
        MethodNode mn;
        if (dynamicListener.getListenerMethod().getReturnType().getName().equals("void")) {
          mn = createVoidListenerMethod(dynamicListener);
        } else {
          mn = createMutableListenerCallbackMethod(dynamicListener);
        }
        cn.methods.add(mn);
      }
      ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS);
      cn.accept(cw);
      this.completed = true;
      return cw.toByteArray();
    } catch (Throwable th) {
      Bctrace.getAgentLogger().setLevel(Level.ERROR);
      Bctrace.getAgentLogger()
          .log(Level.ERROR, "Error found instrumenting class " + className, th);
      th.printStackTrace();
      return null;
    }
  }

  public static String getDynamicListenerMethodName(DirectListener listener) {
    return listener.getClass().getName().replace('.', '_') + "_" +
        listener.getListenerMethod().getName();
  }

  /**
   * Generates the following method in Callback class:
   *
   * <code>
   * <pre>
   * public static void ${dynamicMethodName}(int i, ${listenerMethodArgs ...}) {
   *   // 1
   *   if (!CallbackEnabler.isThreadNotificationEnabled()) {
   *     return;
   *   }
   *   // 2
   *   if (Boolean.TRUE == NOTIFYING_FLAG.get()) {
   *     return;
   *   }
   *   try {
   *   // 3
   *     NOTIFYING_FLAG.set(Boolean.TRUE);
   *     ((${DinamicListenerInterface})listeners[i]).${method}(${listenerMethodArgs});
   *     NOTIFYING_FLAG.set(Boolean.FALSE);
   *   } catch (Throwable th) {
   *   // 4
   *     handleThrowable(th);
   *     NOTIFYING_FLAG.set(Boolean.FALSE);
   *   }
   * }
   * </pre>
   * </code>
   */
  private static MethodNode createVoidListenerMethod(DirectListener listener) {
    Method listenerMethod = listener.getListenerMethod();
    MethodNode mn = new MethodNode(
        (Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC),
        getDynamicListenerMethodName(listener),
        null,
        null,
        null
    );
    updateVoidDescriptor(mn, listenerMethod);
    InsnList insnList = mn.instructions;

    // 1
    insnList.add(
        new MethodInsnNode(Opcodes.INVOKESTATIC, "io/shiftleft/bctrace/runtime/CallbackEnabler",
            "isThreadNotificationEnabled", "()Z", false));
    LabelNode ln = new LabelNode();
    insnList.add(new JumpInsnNode(Opcodes.IFNE, ln));
    insnList.add(new InsnNode(Opcodes.RETURN));
    insnList.add(ln);
    insnList.add(new FrameNode(Opcodes.F_SAME, 0, null, 0, null));

    // 2
    insnList.add(
        new FieldInsnNode(Opcodes.GETSTATIC, "java/lang/Boolean", "TRUE", "Ljava/lang/Boolean;"));
    insnList.add(new FieldInsnNode(Opcodes.GETSTATIC, "io/shiftleft/bctrace/runtime/Callback",
        "NOTIFYING_FLAG", "Ljava/lang/ThreadLocal;"));
    insnList.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/ThreadLocal", "get",
        "()Ljava/lang/Object;", false));
    ln = new LabelNode();
    insnList.add(new JumpInsnNode(Opcodes.IF_ACMPNE, ln));
    insnList.add(new InsnNode(Opcodes.RETURN));
    insnList.add(ln);
    insnList.add(new FrameNode(Opcodes.F_SAME, 0, null, 0, null));

    // 3
    LabelNode start = new LabelNode();
    LabelNode end = new LabelNode();
    LabelNode handler = new LabelNode();
    mn.tryCatchBlocks.add(new TryCatchBlockNode(start, end, handler, null));

    insnList.add(start);
    addSetNotifyingFlagInstructions(insnList, "TRUE");
    insnList.add(new FieldInsnNode(Opcodes.GETSTATIC, "io/shiftleft/bctrace/runtime/Callback",
        "listeners", "[Ljava/lang/Object;"));
    insnList.add(new VarInsnNode(Opcodes.ILOAD, 0));
    insnList.add(new InsnNode(Opcodes.AALOAD));

    String interfaceType = Type.getType(listenerMethod.getDeclaringClass())
        .getInternalName() + "Interface";
    insnList.add(new TypeInsnNode(Opcodes.CHECKCAST, interfaceType));
    Class<?>[] params = listenerMethod.getParameterTypes();
    StringBuilder descriptor = new StringBuilder("(");
    for (int i = 0; i < params.length; i++) {
      Type type = Type.getType(params[i]);
      insnList.add(ASMUtils.getLoadInst(type, i + 1));
      descriptor.append(type.getDescriptor());
    }
    descriptor.append(")V");
    insnList.add(
        new MethodInsnNode(Opcodes.INVOKEINTERFACE, interfaceType, listenerMethod.getName(),
            descriptor.toString(), true));
    addSetNotifyingFlagInstructions(insnList, "FALSE");
    insnList.add(new InsnNode(Opcodes.RETURN));
    insnList.add(end);

    // 4
    insnList.add(handler);
    insnList.add(new FrameNode(Opcodes.F_SAME1, 0, null, 1,
        new Object[]{"java/lang/Throwable"}));
    insnList.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "io/shiftleft/bctrace/runtime/Callback",
        "handleThrowable", "(Ljava/lang/Throwable;)V", false));
    addSetNotifyingFlagInstructions(insnList, "FALSE");
    insnList.add(new InsnNode(Opcodes.RETURN));
    return mn;
  }

  /**
   * Generates the following method in Callback class:
   *
   * <code>
   * <pre>
   * public static ${listenerReturnType} ${dynamicMethodName}(int i, ${listenerReturnType} value, ${listenerMethodArgs...}) {
   *   // 1
   *   if (!CallbackEnabler.isThreadNotificationEnabled()) {
   *     return value;
   *   }
   *   // 2
   *   if (Boolean.TRUE == NOTIFYING_FLAG.get()) {
   *     return value;
   *   }
   *   ${listenerReturnType} ret = value;
   *   try {
   *   // 3
   *     NOTIFYING_FLAG.set(Boolean.TRUE);
   *     ret = ((${DinamicListenerInterface})listeners[i]).${method}(${listenerMethodArgs});
   *     NOTIFYING_FLAG.set(Boolean.FALSE);
   *     return ret;
   *   } catch (Throwable th) {
   *   // 4
   *     handleThrowable(th);
   *     NOTIFYING_FLAG.set(Boolean.FALSE);
   *     return ret;
   *   }
   * }
   * </pre>
   * </code>
   */
  private MethodNode createMutableListenerCallbackMethod(DirectListener listener) {
    Method listenerMethod = listener.getListenerMethod();
    Type returnType = Type.getReturnType(listenerMethod);
    MethodNode mn = new MethodNode(
        (Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC),
        getDynamicListenerMethodName(listener),
        null,
        null,
        null
    );
    updateMutableDescriptor(mn, listenerMethod);
    InsnList insnList = mn.instructions;

    // 1
    insnList.add(
        new MethodInsnNode(Opcodes.INVOKESTATIC, "io/shiftleft/bctrace/runtime/CallbackEnabler",
            "isThreadNotificationEnabled", "()Z", false));
    LabelNode ln = new LabelNode();
    insnList.add(new JumpInsnNode(Opcodes.IFNE, ln));
    insnList.add(ASMUtils.getLoadInst(returnType, 1)); // value
    insnList.add(ASMUtils.getReturnInst(returnType));
    insnList.add(ln);
    insnList.add(new FrameNode(Opcodes.F_SAME, 0, null, 0, null));

    // 2
    insnList.add(
        new FieldInsnNode(Opcodes.GETSTATIC, "java/lang/Boolean", "TRUE", "Ljava/lang/Boolean;"));
    insnList.add(new FieldInsnNode(Opcodes.GETSTATIC, "io/shiftleft/bctrace/runtime/Callback",
        "NOTIFYING_FLAG", "Ljava/lang/ThreadLocal;"));
    insnList.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/ThreadLocal", "get",
        "()Ljava/lang/Object;", false));
    ln = new LabelNode();
    insnList.add(new JumpInsnNode(Opcodes.IF_ACMPNE, ln));
    insnList.add(ASMUtils.getLoadInst(returnType, 1)); // value
    insnList.add(ASMUtils.getReturnInst(returnType));
    insnList.add(ln);
    insnList.add(new FrameNode(Opcodes.F_SAME, 0, null, 0, null));
    int returnVarIndex = computeInitialMaxLocals(mn.desc);
    insnList.add(ASMUtils.getLoadInst(returnType, 1)); // value
    insnList.add(ASMUtils.getStoreInst(returnType, returnVarIndex)); // ret

    // 3
    LabelNode start = new LabelNode();
    LabelNode end = new LabelNode();
    LabelNode handler = new LabelNode();
    mn.tryCatchBlocks.add(new TryCatchBlockNode(start, end, handler, null));

    insnList.add(start);
    addSetNotifyingFlagInstructions(insnList, "TRUE");
    insnList.add(new FieldInsnNode(Opcodes.GETSTATIC, "io/shiftleft/bctrace/runtime/Callback",
        "listeners", "[Ljava/lang/Object;"));
    insnList.add(new VarInsnNode(Opcodes.ILOAD, 0));
    insnList.add(new InsnNode(Opcodes.AALOAD));

    String interfaceType = Type.getType(listenerMethod.getDeclaringClass())
        .getInternalName() + "Interface";
    insnList.add(new TypeInsnNode(Opcodes.CHECKCAST, interfaceType));
    Class<?>[] params = listenerMethod.getParameterTypes();
    StringBuilder descriptor = new StringBuilder("(");
    for (int i = 0; i < params.length; i++) {
      Type type = Type.getType(params[i]);
      insnList.add(ASMUtils.getLoadInst(type, i + 2));
      descriptor.append(type.getDescriptor());
    }
    descriptor.append(")").append(returnType.getDescriptor());
    insnList.add(
        new MethodInsnNode(Opcodes.INVOKEINTERFACE, interfaceType, listenerMethod.getName(),
            descriptor.toString(), true));

    insnList.add(ASMUtils.getStoreInst(returnType, returnVarIndex));
    addSetNotifyingFlagInstructions(insnList, "FALSE");
    insnList.add(ASMUtils.getLoadInst(returnType, returnVarIndex));
    insnList.add(ASMUtils.getReturnInst(returnType));
    insnList.add(end);

    // 4
    insnList.add(handler);
    insnList.add(new FrameNode(Opcodes.F_SAME1, 0, null, 1,
        new Object[]{"java/lang/Throwable"}));
    insnList.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "io/shiftleft/bctrace/runtime/Callback",
        "handleThrowable", "(Ljava/lang/Throwable;)V", false));
    addSetNotifyingFlagInstructions(insnList, "FALSE");
    insnList.add(ASMUtils.getLoadInst(returnType, returnVarIndex));
    insnList.add(ASMUtils.getReturnInst(returnType));
    return mn;
  }

  private static void addSetNotifyingFlagInstructions(InsnList insnList, String value) {
    insnList.add(new FieldInsnNode(Opcodes.GETSTATIC, "io/shiftleft/bctrace/runtime/Callback",
        "NOTIFYING_FLAG", "Ljava/lang/ThreadLocal;"));
    insnList.add(
        new FieldInsnNode(Opcodes.GETSTATIC, "java/lang/Boolean", value, "Ljava/lang/Boolean;"));
    insnList.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/ThreadLocal", "set",
        "(Ljava/lang/Object;)V", false));
  }

  private int computeInitialMaxLocals(String methodDescriptor) {
    Type[] args = Type.getArgumentTypes(methodDescriptor);
    int ret = 0;
    for (int i = 0; i < args.length; i++) {
      ret += args[i].getSize();
    }
    return ret;
  }

  public boolean isCompleted() {
    return completed;
  }

  public static String getDynamicListenerVoidMethodDescriptor(DirectListener listener) {
    return updateVoidDescriptor(null, listener.getListenerMethod());
  }

  public static String getDynamicListenerMutatorMethodDescriptor(DirectListener listener) {
    return updateMutableDescriptor(null, listener.getListenerMethod());
  }

  private static String updateVoidDescriptor(MethodNode mn, Method listenerMethod) {
    StringBuilder descriptor = new StringBuilder();
    descriptor.append("(");
    // First parameter is the hook id
    descriptor.append("I");
    // Next parameters are the ones of the listener
    Class<?>[] params = listenerMethod.getParameterTypes();
    for (int i = 0; i < params.length; i++) {
      String paramDesc = Type.getDescriptor(params[i]);
      descriptor.append(paramDesc);
      if (mn != null) {
        mn.localVariables
            .add(new LocalVariableNode("arg" + (i + 1), paramDesc, null,
                new LabelNode(),
                new LabelNode(), i + 1));
      }
    }
    descriptor.append(")V");
    String desc = descriptor.toString();
    if (mn != null) {
      mn.desc = desc;
    }
    return desc;
  }

  private static String updateMutableDescriptor(MethodNode mn, Method listenerMethod) {
    StringBuilder descriptor = new StringBuilder();
    descriptor.append("(");
    // First parameter is the hook id
    descriptor.append("I");
    // Second parameter is the original value pop from the stack
    descriptor.append(Type.getReturnType(listenerMethod).getDescriptor());
    // Next parameters are the ones of the listener
    Class<?>[] params = listenerMethod.getParameterTypes();
    for (int i = 0; i < params.length; i++) {
      String paramDesc = Type.getDescriptor(params[i]);
      descriptor.append(paramDesc);
      if (mn != null) {
        mn.localVariables
            .add(new LocalVariableNode("arg" + (i + 1), paramDesc, null,
                new LabelNode(),
                new LabelNode(), i + 1));
      }
    }
    descriptor.append(")");
    descriptor.append(Type.getReturnType(listenerMethod).getInternalName());
    String desc = descriptor.toString();
    if (mn != null) {
      mn.desc = desc;
    }
    return desc;
  }

}
