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
import java.util.List;
import io.shiftleft.bctrace.asm.helper.CatchHelper;
import io.shiftleft.bctrace.asm.helper.MinStartHelper;
import io.shiftleft.bctrace.asm.helper.ReturnHelper;
import io.shiftleft.bctrace.asm.helper.StartArgumentsHelper;
import io.shiftleft.bctrace.asm.helper.StartHelper;
import io.shiftleft.bctrace.asm.helper.ThrowHelper;
import io.shiftleft.bctrace.asm.util.ASMUtils;
import io.shiftleft.bctrace.debug.DebugInfo;
import io.shiftleft.bctrace.runtime.Callback;
import io.shiftleft.bctrace.runtime.MethodInfo;
import io.shiftleft.bctrace.runtime.MethodRegistry;
import io.shiftleft.bctrace.spi.Hook;
import io.shiftleft.bctrace.spi.SystemProperties;
import io.shiftleft.bctrace.spi.listener.info.BeforeThrownListener;
import io.shiftleft.bctrace.spi.listener.info.FinishReturnListener;
import io.shiftleft.bctrace.spi.listener.info.FinishThrowableListener;
import io.shiftleft.bctrace.spi.listener.Listener;
import io.shiftleft.bctrace.spi.listener.info.StartArgumentsListener;
import io.shiftleft.bctrace.spi.listener.info.StartListener;
import io.shiftleft.bctrace.spi.listener.min.MinStartListener;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;
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

  private static final File DUMP_FOLDER;
  private static final AtomicInteger TRANSFORMATION_COUNTER = new AtomicInteger();

  static {
    if (System.getProperty(SystemProperties.DUMP_FOLDER) != null) {
      File file = new File(System.getProperty(SystemProperties.DUMP_FOLDER));
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

  @Override
  public byte[] transform(final ClassLoader loader,
          final String className, final Class<?> classBeingRedefined,
          final ProtectionDomain protectionDomain,
          final byte[] classfileBuffer)
          throws IllegalClassFormatException {

    if (className == null) {
      return null;
    }
    int counter = TRANSFORMATION_COUNTER.incrementAndGet();

    byte[] ret = null;
    try {
      // Do not instrument agent classes (non bootstrap classloader only)
      if (protectionDomain != null && protectionDomain.equals(getClass().getProtectionDomain())) {
        return ret;
      }
      if (classfileBuffer == null) {
        return ret;
      }
      if (!TransformationSupport.isTransformable(className)) {
        return ret;
      }
      ArrayList<Integer> matchingHooks = getMatchingHooks(className, protectionDomain, loader);
      if (matchingHooks == null || matchingHooks.isEmpty()) {
        return ret;
      }

      ClassReader cr = new ClassReader(classfileBuffer);
      ClassNode cn = new ClassNode();
      cr.accept(cn, 0);

      boolean transformed = transformMethods(cn, matchingHooks);
      if (!transformed) {
        return ret;
      } else {
        ClassWriter cw = new StaticClassWriter(cr, ClassWriter.COMPUTE_FRAMES, loader);
        cn.accept(cw);
        ret = cw.toByteArray();
        return ret;
      }
    } catch (Throwable th) {
      handle(th);
      return ret;
    } finally {
      if (DUMP_FOLDER != null) {
        dump(counter, className, classfileBuffer, ret);
      }
    }
  }

  private static void handle(Throwable th) {
    Hook[] hooks = Callback.hooks;
    if (hooks != null) {
      for (Hook hook : hooks) {
        if (hook != null) {
          hook.onError(th);
        }
      }
    }
  }

  private static void dump(int counter, String className, byte[] original, byte[] transformed) {
    try {
      if (transformed != null) {
        FileOutputStream fos = new FileOutputStream(new File(DUMP_FOLDER, counter + "#" + className.replace('/', '.') + "(input).class"));
        fos.write(original);
        fos.close();
        fos = new FileOutputStream(new File(DUMP_FOLDER, counter + "#" + className.replace('/', '.') + "(output).class"));
        fos.write(transformed);
        fos.close();
      } else {
        FileOutputStream fos = new FileOutputStream(new File(DUMP_FOLDER, "noop.txt"), true);
        fos.write((counter + "#" + className).getBytes());
        fos.write("\n".getBytes());
        fos.close();
      }
    } catch (Exception ex) {
      handle(ex);
    }
  }

  private ArrayList<Integer> getMatchingHooks(String className, ProtectionDomain protectionDomain, ClassLoader loader) {
    ArrayList<Integer> ret = new ArrayList<Integer>(Callback.hooks.length);
    Hook[] hooks = Callback.hooks;
    if (hooks != null) {
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
      if (ASMUtils.isAbstract(mn.access) || ASMUtils.isNative(mn.access)) {
        continue;
      }
      if (mn.name.equals("<init>") || mn.name.equals("<cinit>")) {
        continue;
      }
      ArrayList<Integer> hooksToUse = new ArrayList<Integer>(matchingHooks.size());
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
        if (DebugInfo.getInstance() != null) {
          Integer methodId = MethodRegistry.getInstance().getMethodId(cn.name, mn.name, mn.desc);
          DebugInfo.getInstance().setInstrumented(methodId, true);
        }
      } else {
        if (DebugInfo.getInstance() != null) {
          Integer methodId = MethodRegistry.getInstance().getMethodId(cn.name, mn.name, mn.desc);
          DebugInfo.getInstance().setInstrumented(methodId, false);
        }
      }
    }
    return transformed;
  }

  private void modifyMethod(ClassNode cn, MethodNode mn, ArrayList<Integer> hooksToUse) {

    Integer methodId = MethodRegistry.getInstance().registerMethodId(MethodInfo.from(cn, mn));

    if (DebugInfo.getInstance() != null) {
      DebugInfo.getInstance().setInstrumented(methodId, true);
    }

    ArrayList<Integer> minStartListenerHooks = getListenerHooks(hooksToUse, MinStartListener.class);
    ArrayList<Integer> startListenerHooks = getListenerHooks(hooksToUse, StartListener.class);
    ArrayList<Integer> startArgumentsListenerHooks = getListenerHooks(hooksToUse, StartArgumentsListener.class);
    ArrayList<Integer> finishReturnListenerHooks = getListenerHooks(hooksToUse, FinishReturnListener.class);
    ArrayList<Integer> finishThrowableListenerHooks = getListenerHooks(hooksToUse, FinishThrowableListener.class);
    ArrayList<Integer> beforeThrownListenerHooks = getListenerHooks(hooksToUse, BeforeThrownListener.class);

    LabelNode startNode = CatchHelper.insertStartNode(mn, finishThrowableListenerHooks);
    MinStartHelper.addTraceStart(methodId, cn, mn, minStartListenerHooks);
    StartHelper.addTraceStart(methodId, cn, mn, startListenerHooks);
    StartArgumentsHelper.addTraceStart(methodId, cn, mn, startArgumentsListenerHooks);
    ReturnHelper.addTraceReturn(methodId, mn, finishReturnListenerHooks);
    ThrowHelper.addTraceThrow(methodId, mn, beforeThrownListenerHooks);
    CatchHelper.addTraceThrowableUncaught(methodId, mn, startNode, finishThrowableListenerHooks);
  }

  private static ArrayList<Integer> getListenerHooks(ArrayList<Integer> hooksToUse, Class<? extends Listener> clazz) {
    ArrayList<Integer> ret = null;
    for (Integer i : hooksToUse) {
      Hook hook = Callback.hooks[i];
      if (hook.getListener() != null && clazz.isAssignableFrom(hook.getListener().getClass())) {
        if (ret == null) {
          ret = new ArrayList<Integer>(hooksToUse.size());
        }
        ret.add(i);
      }
    }
    return ret;
  }
}
