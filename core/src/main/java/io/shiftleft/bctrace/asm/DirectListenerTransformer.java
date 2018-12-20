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
package io.shiftleft.bctrace.asm;

import io.shiftleft.bctrace.Bctrace;
import io.shiftleft.bctrace.Instrumentation;
import io.shiftleft.bctrace.asm.util.ASMUtils;
import io.shiftleft.bctrace.asm.util.Unsafe;
import io.shiftleft.bctrace.hierarchy.UnloadedClass;
import io.shiftleft.bctrace.logging.Level;
import io.shiftleft.bctrace.runtime.CallbackEnabler;
import io.shiftleft.bctrace.runtime.listener.direct.DirectListener;
import io.shiftleft.bctrace.runtime.listener.direct.DirectListener.ListenerMethod;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.List;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodNode;

/**
 * Creates an interface for each DirectListener and makes the listener implement it.
 *
 * @author Ignacio del Valle Alles idelvall@shiftleft.io
 */
public class DirectListenerTransformer implements ClassFileTransformer {

  private static final String LISTENER_METHOD_ANNOTATION_DESC =
      Type.getDescriptor(ListenerMethod.class);

  private static final InsnList EMPTY_INSN_LIST = new InsnList();
  private static final List<String> EMPTY_LIST = new ArrayList<String>();


  private final Instrumentation instrumentation;

  public DirectListenerTransformer(Instrumentation instrumentation) {
    this.instrumentation = instrumentation;
  }

  @Override
  public byte[] transform(final ClassLoader loader,
      final String className,
      final Class<?> classBeingRedefined,
      final ProtectionDomain protectionDomain,
      final byte[] classfileBuffer)
      throws IllegalClassFormatException {

    try {
      CallbackEnabler.disableThreadNotification();
      if (className == null) {
        return null;
      }
      ClassReader cr = new ClassReader(classfileBuffer);
      ClassNode cn = new ClassNode();
      cr.accept(cn, 0);
      UnloadedClass ci = new UnloadedClass(className.replace('/', '.'), loader, cn,
          instrumentation);
      if (ci.isInstanceOf(DirectListener.class.getName())) {
        makeDirectListenerImplementInterface(cn);
        ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS);
        cn.accept(cw);
        return cw.toByteArray();

      }
      return null;
    } catch (Throwable th) {
      Bctrace.getAgentLogger().setLevel(Level.ERROR);
      Bctrace.getAgentLogger()
          .log(Level.ERROR, "Error found instrumenting class " + className, th);
      return null;
    } finally {
      CallbackEnabler.enableThreadNotification();
    }
  }

  /**
   * @return interface class created in bootstrap classloader
   */
  private static Class makeDirectListenerImplementInterface(ClassNode directListenerClassNode) {
    ClassNode itf = new ClassNode();
    itf.name = ASMUtils.getJvmInterfaceNameForDirectListener(directListenerClassNode.name);
    itf.access =
        Opcodes.ACC_PUBLIC | Opcodes.ACC_INTERFACE | Opcodes.ACC_ABSTRACT | Opcodes.ACC_SYNTHETIC;
    itf.version = Opcodes.V1_6;
    itf.superName = "java/lang/Object";

    // Make listener implement new interface
    directListenerClassNode.interfaces.add(itf.name);
    if (directListenerClassNode.methods != null) {
      for (MethodNode mn : directListenerClassNode.methods) {
        if (mn.visibleAnnotations != null) {
          for (AnnotationNode an : mn.visibleAnnotations) {
            if (LISTENER_METHOD_ANNOTATION_DESC.equals(an.desc)) {
              copyMethodToInterface(itf, mn);
              break;
            }
          }
        }
      }
    }
    ClassWriter icw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
    itf.accept(icw);
    return Unsafe.defineClass(itf.name.replace('/', '.'), icw.toByteArray(), null, null);
  }

  private static void copyMethodToInterface(ClassNode itf, MethodNode mn) {
    MethodNode imn = new MethodNode();
    imn.access = Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT;
    imn.name = mn.name;
    imn.desc = mn.desc;
    imn.instructions = EMPTY_INSN_LIST;
    imn.exceptions = EMPTY_LIST;
    imn.signature = mn.signature;
    // Make listener method public (if not yet)
    mn.access = Opcodes.ACC_PUBLIC;
    itf.methods.add(imn);
  }
}
