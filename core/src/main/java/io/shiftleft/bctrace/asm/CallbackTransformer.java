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

import io.shiftleft.bctrace.asm.util.ASMUtils;
import io.shiftleft.bctrace.hook.DynamicHook;
import io.shiftleft.bctrace.hook.Hook;
import io.shiftleft.bctrace.runtime.listener.specific.DynamicListener;
import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.reflect.Method;
import java.security.ProtectionDomain;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;

/**
 * @author Ignacio del Valle Alles idelvall@shiftleft.io
 */
public class CallbackTransformer implements ClassFileTransformer {

  private static final String CALLBACK_JVM_CLASS_NAME = "io/shiftleft/bctrace/runtime/Callback";
  private final Hook[] hooks;

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
      Set<DynamicListener> dynamicListeners = new HashSet<DynamicListener>();
      for (int i = 0; i < this.hooks.length; i++) {
        if (this.hooks[i] instanceof DynamicHook) {
          DynamicHook dynamicHook = (DynamicHook) this.hooks[i];
          dynamicListeners.add(dynamicHook.getListener());
        }
      }
      if (dynamicListeners.size() == 0) {
        return null;
      }
      ClassReader cr = new ClassReader(
          ClassLoader.getSystemResourceAsStream(CALLBACK_JVM_CLASS_NAME + ".class"));
      ClassNode cn = new ClassNode();
      cn.version = Opcodes.V1_6;
      cr.accept(cn, 0);

      MethodNode templateMethodNode = null;
      List<MethodNode> methodNodes = cn.methods;
      for (MethodNode methodNode : methodNodes) {
        if (methodNode.name.equals("dynamicTemplate")) {
          templateMethodNode = methodNode;
          break;
        }
      }
      cn.methods.remove(templateMethodNode);
      HashSet<String> methodsAdded = new HashSet<String>();
      for (DynamicListener dynamicListener : dynamicListeners) {
        MethodNode mn = createCallbackMethod(templateMethodNode, dynamicListener);
        String key = mn.name + mn.desc;
        if (!methodsAdded.contains(key)) {
          cn.methods.add(mn);
          methodsAdded.add(key);
        }
      }

      ClassWriter cw = new StaticClassWriter(cr, ClassWriter.COMPUTE_MAXS, null);
      cn.accept(cw);
      return cw.toByteArray();
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }

  public static String getDynamicListenerMethodName(DynamicListener listener) {
    return listener.getClass().getName().replace('.', '_') + "_" +
        listener.getListenerMethod().getName();
  }

  private static MethodNode createCallbackMethod(MethodNode templateMethod,
      DynamicListener listener) {
    MethodNode mn = ASMUtils.cloneMethod(templateMethod);
    // Change method name from the template method
    mn.name = getDynamicListenerMethodName(listener);
    // Change signature of the method from private to public
    mn.access = (mn.access | Opcodes.ACC_PUBLIC) & ~Opcodes.ACC_PRIVATE;
    updateMethodDescriptor(mn, listener.getListenerMethod());
    updateMethodByteCode(mn, listener.getListenerMethod());
    return mn;
  }

  public static String getDynamicListenerMethodDescriptor(DynamicListener listener) {
    return updateMethodDescriptor(null, listener.getListenerMethod());
  }

  private static String updateMethodDescriptor(MethodNode mn, Method listenerMethod) {
    StringBuilder descriptor = new StringBuilder();
    descriptor.append("(");
    // First parameter is the methodId integer
    descriptor.append("I");
    // Next parameters are the ones of the listener
    Class<?>[] params = listenerMethod.getParameterTypes();
    for (int i = 0; i < params.length; i++) {
      String paramDesc = Type.getDescriptor(params[i]);
      descriptor.append(paramDesc);
      if (mn != null) {
        mn.localVariables
            .add(new LocalVariableNode("arg" + (i + 1), paramDesc, null,
                mn.localVariables.get(0).start,
                mn.localVariables.get(0).end, i + 1));
      }
    }
    descriptor.append(")");
    // Void return
    descriptor.append("V");
    String desc = descriptor.toString();
    if (mn != null) {
      mn.desc = desc;
    }
    return desc;
  }

  // Changes toString() by listenerMethod(arg1, arg2, ...)
  private static void updateMethodByteCode(MethodNode mn, Method listenerMethod) {
    //
    InsnList listenerParams = new InsnList();
    listenerParams.add(new TypeInsnNode(Opcodes.CHECKCAST,
        Type.getInternalName(listenerMethod.getDeclaringClass())));
    Class<?>[] params = listenerMethod.getParameterTypes();
    StringBuilder descriptor = new StringBuilder("(");
    for (int i = 0; i < params.length; i++) {
      Type type = Type.getType(params[i]);
      listenerParams.add(ASMUtils.getLoadInst(type, i + 1));
      descriptor.append(type.getDescriptor());
    }
    descriptor.append(")V");
    InsnList instructions = mn.instructions;
    MethodInsnNode listenerCallSite = null;
    for (int i = 0; i < instructions.size(); i++) {
      AbstractInsnNode node = instructions.get(i);
      if (node instanceof MethodInsnNode) {
        MethodInsnNode methodCallSite = (MethodInsnNode) node;
        if (methodCallSite.name.equals("notify")) {
          listenerCallSite = methodCallSite;
          listenerCallSite.owner = Type.getType(listenerMethod.getDeclaringClass())
              .getInternalName();
          listenerCallSite.name = listenerMethod.getName();
          listenerCallSite.desc = descriptor.toString();
          break;
        }
      }
    }
    instructions.insertBefore(listenerCallSite, listenerParams);
  }
}
