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

import io.shiftleft.bctrace.runtime.InstrumentationImpl;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import io.shiftleft.bctrace.asm.helper.StartHelper;
import io.shiftleft.bctrace.asm.utils.ASMUtils;
import io.shiftleft.bctrace.runtime.Callback;
import io.shiftleft.bctrace.spi.Hook;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodNode;

/**
 *
 * @author Ignacio del Valle Alles idelvall@shiftleft.io
 */
public class Transformer implements ClassFileTransformer {

  private static final String[] NO_MODIFICATION = new String[] {
      "io/shiftleft/bctrace/",
      "java/lang/",
      "sun/",
      "com/sun/",
      "javafx/",
      "io/shiftleft/agent",
      "oracle/"
  };

  @Override
  public byte[] transform(final ClassLoader loader,
          final String className, final Class<?> classBeingRedefined,
          final ProtectionDomain protectionDomain,
          final byte[] classfileBuffer)
          throws IllegalClassFormatException {

    try {
      // Do not instrument agent classes
      if (protectionDomain != null && protectionDomain.equals(getClass().getProtectionDomain())) {
        return null;
      }
      if (className == null || classfileBuffer == null) {
        return null;
      }
      for(String shouldNotChange: NO_MODIFICATION) {
        if (className.startsWith(shouldNotChange)) {
          return null;
        }
      }

      ArrayList<Integer> matchingHooks = getMatchingHooks(className, protectionDomain, loader);
      if (matchingHooks == null || matchingHooks.isEmpty()) {
        return null;
      }

      ClassReader cr = new ClassReader(classfileBuffer);
      ClassNode cn = new ClassNode();
      cr.accept(cn, 0);

      boolean transformed = transformMethods(cn, matchingHooks);
      if (!transformed) {
        return null;
      } else {
        ClassWriter cw = new StaticClassWriter(cr,
            ClassWriter.COMPUTE_FRAMES|ClassWriter.COMPUTE_MAXS, loader);
        cn.accept(cw);
        return cw.toByteArray();
      }
    } catch (Throwable th) {
      Hook[] hooks = Callback.hooks;
      if (hooks != null) {
        for (Hook hook : hooks) {
          if (hook != null) {
            hook.onError(th);
          }
        }
      }
      return null;
    }
  }

  private ArrayList<Integer> getMatchingHooks(String className, ProtectionDomain protectionDomain,
      ClassLoader loader) {
    ArrayList<Integer> ret = null;
    Hook[] hooks = Callback.hooks;
    if (hooks != null) {
      ret = new ArrayList<Integer>(Callback.hooks.length);
      for (int i = 0; i < hooks.length; i++) {
        if (className.startsWith(hooks[i].getJvmPackage())) {
          return null;
        }
        if (hooks[i].getFilter().instrumentClass(className, protectionDomain, loader)) {
          ret.add(i);
        }
        ((InstrumentationImpl) hooks[i].getInstrumentation()).removeTransformedClass(className);
      }
    }
    return ret;
  }

  private boolean transformMethods(ClassNode cn, ArrayList<Integer> matchingHooks) {
    List<MethodNode> methods = cn.methods;
    boolean transformed = false;
    for (MethodNode mn : methods) {
      if (ASMUtils.isAbstract(mn) || ASMUtils.isNative(mn)) {
        continue;
      }
      if (mn.name.equals("<init>") || mn.name.equals("<cinit>") || mn.name.equals("<linit>")) {
        continue;
      }
      ArrayList<Integer> hooksToUse = new ArrayList<Integer>();
      Hook[] hooks = Callback.hooks;
      for (Integer i : matchingHooks) {
        if (hooks[i] != null && hooks[i].getFilter().instrumentMethod(cn, mn)) {
          hooksToUse.add(i);
          ((InstrumentationImpl) hooks[i].getInstrumentation()).addTransformedClass(cn.name.replace('/', '.'));
        }
      }
      if (!hooksToUse.isEmpty()) {
        modifyMethod(cn, mn, hooksToUse);
        transformed = true;
      }
    }
    return transformed;
  }

  private void modifyMethod(ClassNode cn, MethodNode mn, ArrayList<Integer> hooksToUse) {
    StartHelper.addTraceStart(cn, mn, hooksToUse);
  }
}
