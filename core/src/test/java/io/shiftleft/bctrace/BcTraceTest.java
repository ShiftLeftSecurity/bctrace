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
package io.shiftleft.bctrace;

import io.shiftleft.bctrace.asm.Transformer;
import io.shiftleft.bctrace.asm.util.ASMUtils;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import io.shiftleft.bctrace.runtime.Callback;
import io.shiftleft.bctrace.impl.InstrumentationImpl;
import io.shiftleft.bctrace.runtime.listener.Listener;
import io.shiftleft.bctrace.spi.Hook;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.util.Printer;
import org.objectweb.asm.util.Textifier;
import org.objectweb.asm.util.TraceMethodVisitor;

/**
 *
 * @author Ignacio del Valle Alles idelvall@shiftleft.io
 */
public abstract class BcTraceTest {

  public static Class getInstrumentClass(Class clazz, Hook[] hooks) throws Exception {
    Bctrace.instance = new Bctrace(null, hooks);
    String className = clazz.getCanonicalName();
    String resourceName = className.replace('.', '/') + ".class";
    InputStream is = clazz.getClassLoader().getResourceAsStream(resourceName);
    byte[] bytes = ASMUtils.toByteArray(is);
    for (Hook hook : hooks) {
      hook.init(new InstrumentationImpl(null));
    }
    Listener[] listeners = new Listener[hooks.length];
    for (int i = 0; i < listeners.length; i++) {
      listeners[i] = hooks[i].getListener();
    }
    Callback.listeners = listeners;
    Transformer transformer = new Transformer(new InstrumentationImpl(null), "BctraceNativePrefix");
    byte[] newBytes = transformer.transform(null, className, clazz, null, bytes);
    ByteClassLoader cl = new ByteClassLoader();
    return cl.loadClass(className, newBytes);
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

  private static class ByteClassLoader extends ClassLoader {

    public Class<?> loadClass(String name, byte[] byteCode) {
      return super.defineClass(name, byteCode, 0, byteCode.length);
    }
  }
}
