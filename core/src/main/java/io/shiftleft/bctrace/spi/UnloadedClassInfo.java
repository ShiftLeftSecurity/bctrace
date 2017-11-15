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
package io.shiftleft.bctrace.spi;

import io.shiftleft.bctrace.Bctrace;
import io.shiftleft.bctrace.asm.util.ClassInfoCache;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

/**
 * @author Ignacio del Valle Alles idelvall@shiftleft.io
 */
public class UnloadedClassInfo implements HierarchyClassInfo {

  private final ClassNode cn;
  private final ClassLoader cl;

  public UnloadedClassInfo(ClassNode cn, ClassLoader cl) {
    this.cn = cn;
    this.cl = cl;
  }

  public ClassNode getRawClassNode() {
    return cn;
  }

  public HierarchyClassInfo getSuperClass() {
    return UnloadedClassInfo.from(this.cn.superName, this.cl);
  }

  public HierarchyClassInfo[] getInterfaces() {
    List<String> interfaces = this.cn.interfaces;
    HierarchyClassInfo[] ret = new HierarchyClassInfo[interfaces.size()];
    int i = 0;
    for (String interfaceName : interfaces) {
      ret[i] = UnloadedClassInfo.from(interfaceName, cl);
      i++;
    }
    return ret;
  }

  @Override
  public String getName() {
    return cn.name;
  }

  private static HierarchyClassInfo from(String name, ClassLoader cl) {
    if (name == null) {
      return null;
    }
    if (isLoadedByAnyClassLoader(name)) {
      Class clazz = getClassIfLoadedByClassLoader(name, cl);
      if (clazz == null) {
        clazz = getClassIfLoadedByClassLoaderAncestors(name, cl);
      }
      if (clazz == null) {
        try {
          clazz = Class.forName(name, false, cl);
        } catch (ClassNotFoundException e) {
          // Caused by application ClassNotFoundException, so not our problem
          return null;
        }
      }
      if (clazz != null) {
        return new LoadedClassInfo(clazz);
      }
    }

    UnloadedClassInfo.ClassLoaderEntry entry = readClassResource(name + ".class", cl);
    if (entry == null) {
      Bctrace.getInstance().getAgentLogger()
          .warning("Could not obtain class bytecode for unloaded class " + name);

    }
    HierarchyClassInfo ci = ClassInfoCache.getInstance().get(name, entry.cl);
    if (ci == null) {
      ci = new UnloadedClassInfo(createClassNode(entry.is), entry.cl);
      ClassInfoCache.getInstance().add(name, entry.cl, ci);
    }
    return ci;
  }

  private static Class getClassIfLoadedByClassLoader(String name, ClassLoader cl) {
    Class[] allLoadedClasses = Bctrace.getInstance().getInstrumentation().getAllLoadedClasses();
    for (Class clazz : allLoadedClasses) {
      if (clazz.getClassLoader() == cl) {
        if (clazz.getName().equals(name)) {
          return clazz;
        }
      }
    }
    return null;
  }

  private static boolean isLoadedByAnyClassLoader(String name) {
    Class[] allLoadedClasses = Bctrace.getInstance().getInstrumentation().getAllLoadedClasses();
    for (Class clazz : allLoadedClasses) {
      if (clazz.getName().equals(name)) {
        return true;
      }
    }
    return false;
  }

  private static Class getClassIfLoadedByClassLoaderAncestors(String name, ClassLoader cl) {
    if (cl.getParent() != null) {
      Class clazz = getClassIfLoadedByClassLoader(name, cl.getParent());
      if (clazz != null) {
        return clazz;
      } else {
        return getClassIfLoadedByClassLoaderAncestors(name, cl.getParent());
      }
    }
    return null;
  }

  private static UnloadedClassInfo.ClassLoaderEntry readClassResource(String classResource,
      ClassLoader loader) {
    if (loader == null) {
      InputStream is = ClassLoader.getSystemResourceAsStream(classResource);
      if (is == null) {
        return null;
      } else {
        return new UnloadedClassInfo.ClassLoaderEntry(is, loader);
      }
    } else {
      UnloadedClassInfo.ClassLoaderEntry entry = readClassResource(classResource,
          loader.getParent());
      if (entry != null) {
        return entry;
      }
      InputStream is = loader.getResourceAsStream(classResource);
      if (is == null) {
        return null;
      } else {
        return new UnloadedClassInfo.ClassLoaderEntry(is, loader);
      }
    }
  }

  private static ClassNode createClassNode(InputStream is) {
    try {
      ClassReader cr = new ClassReader(is);
      ClassNode cn = new ClassNode();
      cr.accept(cn, 0);
      return cn;
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }

  private static class ClassLoaderEntry {

    InputStream is;
    ClassLoader cl;

    public ClassLoaderEntry(InputStream is, ClassLoader cl) {
      this.is = is;
      this.cl = cl;
    }
  }
}

