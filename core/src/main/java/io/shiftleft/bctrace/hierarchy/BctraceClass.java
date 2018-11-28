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
package io.shiftleft.bctrace.hierarchy;

import io.shiftleft.bctrace.Bctrace;
import io.shiftleft.bctrace.Instrumentation;
import io.shiftleft.bctrace.asm.util.ASMUtils;
import io.shiftleft.bctrace.logging.Level;
import java.net.URL;

/**
 * @author Ignacio del Valle Alles idelvall@shiftleft.io
 */
public abstract class BctraceClass {

  private static final BctraceClass[] EMPTY = new BctraceClass[0];

  protected final Instrumentation inst;
  protected final String name;
  protected final ClassLoader cl;
  private volatile boolean computedURL;
  private volatile URL url;
  protected volatile BctraceClass superClass;
  protected volatile BctraceClass[] interfaces;

  BctraceClass(String name, ClassLoader cl, Instrumentation inst) {
    this.name = name;
    this.cl = cl;
    this.inst = inst;
  }

  protected abstract String getSuperClassName();

  protected abstract String[] getInterfaceNames();


  public final String getName() {
    return name;
  }

  public final URL getURL() {
    if (!computedURL) {
      synchronized (this) {
        this.url = Bctrace.getURL(name, cl);
        computedURL = true;
      }
    }
    return this.url;
  }

  public final BctraceClass getSuperClass() {
    if (superClass == null) {
      synchronized (this) {
        if (superClass == null) {
          String superClassName = getSuperClassName();
          if (superClassName == null) {
            return null;
          }
          superClass = from(superClassName, cl, inst);
        }
      }
    }
    return superClass;
  }

  public final BctraceClass[] getInterfaces() {
    if (interfaces == null) {
      synchronized (this) {
        if (interfaces == null) {
          String[] interfaceNames = getInterfaceNames();
          if (interfaceNames == null || interfaceNames.length == 0) {
            interfaces = EMPTY;
          } else {
            interfaces = new BctraceClass[interfaceNames.length];
            for (int i = 0; i < interfaceNames.length; i++) {
              interfaces[i] = from(interfaceNames[i], cl, inst);
            }
          }
        }
      }
    }
    return interfaces;
  }

  public final boolean isInterface() {
    return ASMUtils.isInterface(getModifiers());
  }

  public final boolean isPublic() {
    return ASMUtils.isPublic(getModifiers());
  }

  public final boolean isPrivate() {
    return ASMUtils.isPrivate(getModifiers());
  }

  public final boolean isProtected() {
    return ASMUtils.isProtected(getModifiers());
  }

  public final boolean isAbstract() {
    return ASMUtils.isAbstract(getModifiers());
  }

  public final boolean isStatic() {
    return ASMUtils.isStatic(getModifiers());
  }

  public abstract int getModifiers();

  /**
   * Factory method that constructs an instance given the class name and classloader.
   *
   * @param name class name according to the Java Language Specification (dot separated)
   */
  public static BctraceClass from(String name, ClassLoader cl, Instrumentation inst) {
    if (name == null) {
      return null;
    }
    if (name.equals("java.lang.Object")) {
      return LoadedClass.OBJECT_CLASS;
    }

    if (inst == null) {
      return null;
    }

    //If the class has been loaded try to create a LoadedClass implementation
    if (inst.isLoadedByAnyClassLoader(name)) {
      // First check if the class has been loaded by this classloader
      Class clazz = inst.getClassIfLoadedByClassLoader(name, cl);
      if (clazz == null) {
        // Then check if the class has been loaded by an ancestor
        clazz = getClassIfLoadedByClassLoaderAncestors(name, cl, inst);
      }
      if (clazz != null) {
        return new LoadedClass(clazz, inst);
      }
    }
    BctraceClass ret;
    try {
      ret = new UnloadedClass(name, cl, inst);
    } catch (ClassNotFoundException ex) {
      Bctrace.getAgentLogger()
          .log(Level.WARNING, "Could not obtain class bytecode for unloaded class " + name);
      ret = new UnresolvedClass(name, cl);
    }
    return ret;
  }

  private static Class getClassIfLoadedByClassLoaderAncestors(String name, ClassLoader cl,
      Instrumentation inst) {
    if (cl != null && cl.getParent() != null) {
      Class clazz = inst.getClassIfLoadedByClassLoader(name, cl.getParent());
      if (clazz != null) {
        return clazz;
      } else {
        return getClassIfLoadedByClassLoaderAncestors(name, cl.getParent(), inst);
      }
    }
    return null;
  }

  public final String getJVMName() {
    return getName().replace('.', '/');
  }

  public final boolean isInstanceOf(String typeName) {
    if (getName().equals(typeName)) {
      return true;
    }
    BctraceClass superClass = getSuperClass();
    if (superClass != null
        && superClass.isInstanceOf(typeName)) {
      return true;
    }
    BctraceClass[] interfaces = getInterfaces();
    if (interfaces != null) {
      for (int i = 0; i < interfaces.length; i++) {
        BctraceClass intf = interfaces[i];
        if (intf != null
            && intf.isInstanceOf(typeName)) {
          return true;
        }
      }
    }
    return false;
  }
}
