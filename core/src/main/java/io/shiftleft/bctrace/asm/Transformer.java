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
import io.shiftleft.bctrace.InstrumentationImpl;
import io.shiftleft.bctrace.MethodInfo;
import io.shiftleft.bctrace.MethodRegistry;
import io.shiftleft.bctrace.SystemProperty;
import io.shiftleft.bctrace.asm.helper.generic.ReturnHelper;
import io.shiftleft.bctrace.asm.helper.generic.StartHelper;
import io.shiftleft.bctrace.asm.helper.generic.ThrowHelper;
import io.shiftleft.bctrace.asm.helper.specific.CallSiteHelper;
import io.shiftleft.bctrace.asm.helper.specific.DirectReturnHelper;
import io.shiftleft.bctrace.asm.helper.specific.DirectStartHelper;
import io.shiftleft.bctrace.asm.util.ASMUtils;
import io.shiftleft.bctrace.debug.DebugInfo;
import io.shiftleft.bctrace.hierarchy.UnloadedClass;
import io.shiftleft.bctrace.hook.GenericHook;
import io.shiftleft.bctrace.hook.Hook;
import io.shiftleft.bctrace.logging.Level;
import io.shiftleft.bctrace.runtime.Callback;
import java.io.File;
import java.io.FileOutputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * @author Ignacio del Valle Alles idelvall@shiftleft.io
 */
public class Transformer implements ClassFileTransformer {

  private static String TRANSFORMATION_SUPPORT_CLASS_NAME = TransformationSupport.class.getName()
      .replace('.', '/');

  private static final File DUMP_FOLDER;
  private final CallbackTransformer cbTransformer;

  private final StartHelper startHelper = new StartHelper();
  private final ReturnHelper returnHelper = new ReturnHelper();
  private final ThrowHelper throwHelper = new ThrowHelper();
  private final CallSiteHelper callSiteHelper = new CallSiteHelper();
  private final DirectStartHelper directStartHelper = new DirectStartHelper();
  private final DirectReturnHelper directReturnHelper = new DirectReturnHelper();

  static {
    if (System.getProperty(SystemProperty.DUMP_FOLDER) != null) {
      File file = new File(System.getProperty(SystemProperty.DUMP_FOLDER));
      if (file.exists() && file.isFile()) {
        file = null;
      }
      DUMP_FOLDER = file;
    } else {
      DUMP_FOLDER = null;
    }
    if (DUMP_FOLDER != null) {
      DUMP_FOLDER.mkdirs();
    }
  }

  private final InstrumentationImpl instrumentation;
  private final Hook[] hooks;
  private final Bctrace bctrace;
  private final AtomicInteger TRANSFORMATION_COUNTER = new AtomicInteger();

  public Transformer(InstrumentationImpl instrumentation, String nativeWrapperPrefix,
      Bctrace bctrace, CallbackTransformer cbTransformer) {
    this.instrumentation = instrumentation;
    this.bctrace = bctrace;
    this.hooks = bctrace.getHooks();
    this.cbTransformer = cbTransformer;

    this.startHelper.setBctrace(bctrace);
    this.returnHelper.setBctrace(bctrace);
    this.throwHelper.setBctrace(bctrace);
    this.callSiteHelper.setBctrace(bctrace);
    this.directStartHelper.setBctrace(bctrace);
    this.directReturnHelper.setBctrace(bctrace);

  }

  @Override
  public byte[] transform(final ClassLoader loader,
      final String className,
      final Class<?> classBeingRedefined,
      final ProtectionDomain protectionDomain,
      final byte[] classfileBuffer)
      throws IllegalClassFormatException {

    // Wait for CallbackTransformer to finish
    if (this.cbTransformer != null && !this.cbTransformer.isCompleted()) {
      return null;
    }

    if (className == null) {
      return null;
    }

    if (DebugInfo.isEnabled()) {
      DebugInfo.getInstance().addInstrumentable(className, loader);
    }
    instrumentation.removeTransformedClass(className.replace('/', '.'), loader);

    int counter = TRANSFORMATION_COUNTER.incrementAndGet();

    byte[] ret = null;
    boolean transformed = false;
    try {
      Callback.disableThreadNotification();
      if (classfileBuffer == null) {
        return ret;
      }
      if (className.equals(TRANSFORMATION_SUPPORT_CLASS_NAME)) {
        return ret;
      }
      if (!TransformationSupport.isTransformable(className, loader)) {
        return ret;
      }
      ArrayList<Integer> matchingHooks = getMatchingHooksByName(className, protectionDomain,
          loader);
      if (matchingHooks == null || matchingHooks.isEmpty()) {
        return ret;
      }
      ClassReader cr = new ClassReader(classfileBuffer);
      ClassNode cn = new ClassNode();
      cr.accept(cn, 0);

      UnloadedClass ci = new UnloadedClass(className.replace('/', '.'), loader, cn, bctrace);

      matchingHooks = getMatchingHooksByClassInfo(matchingHooks, ci, protectionDomain, loader);

      transformed = transformMethods(ci, matchingHooks);
      if (!transformed) {
        return ret;
      } else {
        ClassWriter cw = new StaticClassWriter(cr, ClassWriter.COMPUTE_MAXS, loader);
        cn.accept(cw);
        ret = cw.toByteArray();
        return ret;
      }
    } catch (Throwable th) {
      Bctrace.getAgentLogger()
          .log(Level.ERROR, "Error found instrumenting class " + className, th);
      return ret;
    } finally {
      Callback.enableThreadNotification();
      if (DUMP_FOLDER != null) {
        dump(counter, className, classfileBuffer, ret);
      }
      instrumentation.addLoadedClass(className.replace('/', '.'), loader);
      if (transformed) {
        instrumentation.addTransformedClass(className.replace('/', '.'), loader);
      }
    }
  }

  private static void dump(int counter, String className, byte[] original, byte[] transformed) {
    try {
      if (transformed != null) {
        FileOutputStream fos = new FileOutputStream(
            new File(DUMP_FOLDER, counter + "#" + className.replace('/', '.') + "(input).class"));
        fos.write(original);
        fos.close();
        fos = new FileOutputStream(
            new File(DUMP_FOLDER, counter + "#" + className.replace('/', '.') + "(output).class"));
        fos.write(transformed);
        fos.close();
      } else {
        FileOutputStream fos = new FileOutputStream(new File(DUMP_FOLDER, "noop.txt"), true);
        fos.write((counter + "#" + className).getBytes());
        fos.write("\n".getBytes());
        fos.close();
      }
    } catch (Exception ex) {
      Bctrace.getAgentLogger()
          .log(Level.ERROR, "Error dumping to disk instrumenting class " + className, ex);
    }
  }

  private ArrayList<Integer> getMatchingHooksByName(String className,
      ProtectionDomain protectionDomain,
      ClassLoader loader) {
    if (this.hooks == null) {
      return null;
    }
    ArrayList<Integer> ret = new ArrayList<Integer>(hooks.length);
    for (int i = 0; i < hooks.length; i++) {
      if (hooks[i].getFilter() != null &&
          hooks[i].getFilter().instrumentClass(className, protectionDomain, loader)) {
        ret.add(i);
      }
    }
    return ret;
  }

  private ArrayList<Integer> getMatchingHooksByClassInfo(ArrayList<Integer> candidateHookIndexes,
      UnloadedClass classInfo, ProtectionDomain protectionDomain,
      ClassLoader loader) {

    if (candidateHookIndexes == null) {
      return null;
    }
    ArrayList<Integer> ret = new ArrayList<Integer>(hooks.length);
    for (int i = 0; i < candidateHookIndexes.size(); i++) {
      Integer hookIndex = candidateHookIndexes.get(i);
      if (hooks[hookIndex].getFilter().instrumentClass(classInfo, protectionDomain, loader)) {
        ret.add(hookIndex);
      }
    }
    // Add additional hooks (those who have a null filter and apply only where others are registered)
    if (!ret.isEmpty()) {
      for (int i = 0; i < hooks.length; i++) {
        if (hooks[i].getFilter() == null) {
          ret.add(i);
        }
      }
    }
    return ret;
  }

  private boolean transformMethods(UnloadedClass ci, ArrayList<Integer> matchingHooks) {
    ClassNode cn = ci.getRawClassNode();
    List<MethodNode> methods = cn.methods;
    boolean transformed = false;
    List<MethodNode> newMethods = new ArrayList<MethodNode>();
    for (int m = 0; m < methods.size(); m++) {
      MethodNode mn = methods.get(m);

      ArrayList<Integer> hooksToUse = new ArrayList<Integer>(matchingHooks.size());
      for (int h = 0; h < matchingHooks.size(); h++) {
        Integer i = matchingHooks.get(h);
        if (hooks[i] != null && hooks[i].getFilter() != null && hooks[i].getFilter()
            .instrumentMethod(ci, mn)) {
          hooksToUse.add(i);
        }
      }
      if (ASMUtils.isAbstract(mn.access)) {
        continue;
      }
      if (!hooksToUse.isEmpty()) {
        boolean useGeneric = false;
        // Add additional hooks
        for (int h = 0; h < matchingHooks.size(); h++) {
          Integer i = matchingHooks.get(h);
          if (hooks[i].getFilter() == null) {
            hooksToUse.add(i);
          }
        }
        modifyMethod(cn, mn, hooksToUse, newMethods);
        transformed = true;
        if (DebugInfo.getInstance() != null) {
          Integer methodId = MethodRegistry.getInstance().getMethodId(MethodInfo.from(cn.name, mn));
          DebugInfo.getInstance().setInstrumented(methodId, true);
        }
      } else {
        if (DebugInfo.getInstance() != null) {
          Integer methodId = MethodRegistry.getInstance().getMethodId(MethodInfo.from(cn.name, mn));
          DebugInfo.getInstance().setInstrumented(methodId, false);
        }
      }
    }
    cn.methods.addAll(newMethods);
    return transformed;
  }

  private void modifyMethod(ClassNode cn, MethodNode mn,
      ArrayList<Integer> hooksToUse,
      List<MethodNode> newMethods) {

    boolean hasGenericHooks = false;
    for (int h = 0; h < hooksToUse.size(); h++) {
      Integer i = hooksToUse.get(h);
      Hook hook = hooks[i];
      if (hooks[i] instanceof GenericHook) {
        hasGenericHooks = true;
        break;
      }
    }
    if (hasGenericHooks) {
      Integer methodId = MethodRegistry.getInstance()
          .registerMethodId(MethodInfo.from(cn.name, mn));

      if (DebugInfo.getInstance() != null) {
        DebugInfo.getInstance().setInstrumented(methodId, true);
      }
      startHelper.addByteCodeInstructions(methodId, cn, mn, hooksToUse);
      returnHelper.addByteCodeInstructions(methodId, cn, mn, hooksToUse);
      throwHelper.addByteCodeInstructions(methodId, cn, mn, hooksToUse);
    }
    callSiteHelper.addByteCodeInstructions(cn, mn, hooksToUse);
    directStartHelper.addByteCodeInstructions(cn, mn, hooksToUse);
    directReturnHelper.addByteCodeInstructions(cn, mn, hooksToUse);
  }
}
