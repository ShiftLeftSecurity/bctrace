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
import io.shiftleft.bctrace.asm.util.ASMUtils;
import io.shiftleft.bctrace.logging.Level;
import io.shiftleft.bctrace.runtime.CallbackEnabler;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;

/**
 * Transforms agent classes using java.* classes to use io.shiftleft.java.* instead. This avoids
 * agent triggering class loading of bootstrap classes shared with the target application, that
 * might incur in side effects in some cases (like the case of JUL logging) or
 * java.lang.ClassCircularityError in others (when some class loading is triggered from the agent
 * transformers).
 *
 * This transformation requires BctraceClassLoader being used at runtime, in order to properly
 * resolve the class bytes from the bootstrap classloader
 *
 * @author Ignacio del Valle Alles idelvall@shiftleft.io
 */
public class JavaDecoratorTransformer implements ClassFileTransformer {

  private static final String[] DECORATED_PACKAGE_PREFIXES = {
      "-java/util/concurrent",
      "java/util/"
  };

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
      if (loader != Bctrace.class.getClassLoader()) {
        return null;
      }
      ClassReader cr = new ClassReader(classfileBuffer);
      ClassNode cn = new ClassNode();
      cr.accept(cn, 0);
      decorateClassName(cn);
      cn.signature = decorateSignature(cn.signature);
      decorateHierarchy(cn);
      decorateReferences(cn);
      ClassWriter cw = new ClassWriter(cr, 0);
      cn.accept(cw);
      return cw.toByteArray();
    } catch (Throwable th) {
      Bctrace.getAgentLogger().setLevel(Level.ERROR);
      Bctrace.getAgentLogger()
          .log(Level.ERROR, "Error found instrumenting class " + className, th);
      return null;
    } finally {
      CallbackEnabler.enableThreadNotification();
    }
  }

  private static void decorateClassName(ClassNode cn) {
    if (requiresDecoration(cn.name)) {
      cn.name = decorateClassName(cn.name);
      System.out.println(cn.name);
    }
  }

  private static void decorateHierarchy(ClassNode cn) {
    if (requiresDecoration(cn.superName)) {
      cn.superName = decorateClassName(cn.superName);
    }
    if (cn.interfaces != null) {
      List<String> itfs = cn.interfaces;
      for (int i = 0; i < itfs.size(); i++) {
        String itf = itfs.get(i);
        if (requiresDecoration(itf)) {
          itfs.set(i, decorateClassName(itf));
        }
      }
    }
  }

  private static String decorateSignature(String signature) {
    if (signature == null) {
      return null;
    }
    List<Integer> indexList = null;
    boolean inName = false;
    for (int i = 0; i < signature.length() - 1; i++) {
      if (inName && signature.charAt(i) == ';') {
        inName = false;
      } else if (!inName && signature.charAt(i) == 'L') {
        inName = true;
        pref:
        for (String prefix : DECORATED_PACKAGE_PREFIXES) {
          boolean exclude = false;
          if (prefix.startsWith("-")) {
            prefix = prefix.substring(1);
            exclude = true;
          }
          if (i + 1 + prefix.length() >= signature.length()) {
            continue;
          }
          for (int j = 0; j < prefix.length(); j++) {
            if (signature.charAt(i + 1 + j) != prefix.charAt(j)) {
              continue pref;
            }
          }
          if (exclude) {
            break;
          }
          if (indexList == null) {
            indexList = new ArrayList<Integer>();
          }
          indexList.add(i + 1);
          break;
        }
      }
    }
    if (indexList == null) {
      return signature;
    } else {
      StringBuilder sb = new StringBuilder(signature);
      for (int i = indexList.size() - 1; i >= 0; i--) {
        Integer index = indexList.get(i);
        sb.insert(index, "io/shiftleft/");
      }
      return sb.toString();
    }
  }

  private static void decorateReferences(ClassNode cn) {
    for (FieldNode fn : cn.fields) {
      fn.desc = decorateSignature(fn.desc);
      fn.signature = decorateSignature(fn.signature);
    }
    for (MethodNode mn : cn.methods) {
      mn.desc = decorateSignature(mn.desc);
      mn.signature = decorateSignature(mn.signature);
      if (ASMUtils.isAbstract(mn.access) || ASMUtils.isNative(mn.access)) {
        continue;
      }
      InsnList il = mn.instructions;
      Iterator<AbstractInsnNode> it = il.iterator();
      boolean ret = false;
      while (it.hasNext()) {
        AbstractInsnNode node = it.next();
        if (node instanceof MethodInsnNode) {
          MethodInsnNode callSite = (MethodInsnNode) node;
          callSite.owner = decorateClassName(callSite.owner);
          callSite.desc = decorateSignature(callSite.desc);
        } else if (node instanceof TypeInsnNode) {
          TypeInsnNode typeInsnNode = (TypeInsnNode) node;
          typeInsnNode.desc = decorateClassName(typeInsnNode.desc);
        } else if (node instanceof FieldInsnNode) {
          FieldInsnNode fieldInsnNode = (FieldInsnNode) node;
          fieldInsnNode.owner = decorateClassName(fieldInsnNode.owner);
          fieldInsnNode.desc = decorateSignature(fieldInsnNode.desc);
        }
      }
    }
  }

  private static boolean requiresDecoration(String className) {
    for (String prefix : DECORATED_PACKAGE_PREFIXES) {
      boolean exclude = false;
      if (prefix.startsWith("-")) {
        prefix = prefix.substring(1);
        exclude = true;
      }
      if(exclude){
        if (className.startsWith(prefix)) {
          return false;
        }
      } else if (className.startsWith(prefix)) {
        return true;
      }
    }
    return false;
  }

  private static String decorateClassName(String className) {
    if (requiresDecoration(className)) {
      return "io/shiftleft/" + className;
    }
    return className;
  }
}
