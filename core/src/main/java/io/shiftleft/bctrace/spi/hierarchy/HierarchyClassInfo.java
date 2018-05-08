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
package io.shiftleft.bctrace.spi.hierarchy;

import io.shiftleft.bctrace.Bctrace;
import java.io.IOException;
import java.net.URL;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

/**
 * @author Ignacio del Valle Alles idelvall@shiftleft.io
 */
public abstract class HierarchyClassInfo {

  HierarchyClassInfo() {
  }

  public abstract HierarchyClassInfo getSuperClass();

  public abstract HierarchyClassInfo[] getInterfaces();

  public abstract String getName();

  public abstract ClassLoader getClassLoader();

  public abstract URL getCodeSource();

  public abstract boolean isInterface();

  /**
   * Factory method that constructs an instance given the class name and classloader.
   *
   * @param name class name according to the Java Language Specification (dot separated)
   */
  public static HierarchyClassInfo from(String name, ClassLoader cl) {
    if (name == null) {
      return null;
    }

    //If the class has been loaded try to create a LoadedClassInfo implementation
    if (Bctrace.getInstance().getInstrumentation().isLoadedByAnyClassLoader(name)) {
      // First check if the class has been loaded by this classloader
      Class clazz = Bctrace.getInstance().getInstrumentation()
          .getClassIfLoadedByClassLoader(name, cl);
      if (clazz == null) {
        // Then check if the class has been loaded by an ancestor
        clazz = getClassIfLoadedByClassLoaderAncestors(name, cl);
      }
      if (clazz != null) {
        return new LoadedClassInfo(clazz);
      }
    }
    // If here then try to create a UnloadedClassInfo implementation (read bytecoode as a resource)
    ClassLoaderEntry entry = readClassResource(name.replace('.', '/') + ".class", cl);
    if (entry == null) {
      Bctrace.getAgentLogger()
          .warning("Could not obtain class bytecode for unloaded class " + name);
      return new UnresolvedClassInfo(name);
    }
    // Cannot know the protection domain of an unloaded class (other than the being-instrumented one)
    return new UnloadedClassInfo(createClassNode(entry.url), entry.codeSource, entry.cl);
  }

  private static Class getClassIfLoadedByClassLoaderAncestors(String name, ClassLoader cl) {
    if (cl != null && cl.getParent() != null) {
      Class clazz = Bctrace.getInstance().getInstrumentation()
          .getClassIfLoadedByClassLoader(name, cl.getParent());
      if (clazz != null) {
        return clazz;
      } else {
        return getClassIfLoadedByClassLoaderAncestors(name, cl.getParent());
      }
    }
    return null;
  }

  private static ClassNode createClassNode(URL url) {
    try {
      ClassReader cr = new ClassReader(url.openStream());
      ClassNode cn = new ClassNode();
      cr.accept(cn, 0);
      return cn;
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }

  private static ClassLoaderEntry readClassResource(String classResource,
      ClassLoader loader) {
    if (loader == null) {
      URL url = ClassLoader.getSystemResource(classResource);
      if (url == null) {
        return null;
      } else {
        return new ClassLoaderEntry(url, null, loader);
      }
    } else {
      // parent delegation
      ClassLoaderEntry cle = readClassResource(classResource, loader.getParent());
      if (cle != null) {
        return cle;
      }
      URL url = loader.getResource(classResource);
      if (url == null) {
        return null;
      } else {
        return new ClassLoaderEntry(url, Bctrace.getCodeSource(classResource, url), loader);
      }
    }
  }

  public final String getJVMName() {
    return getName().replace('.', '/');
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof HierarchyClassInfo)) {
      return false;
    }
    HierarchyClassInfo other = (HierarchyClassInfo) obj;
    return this.getName().equals(other.getName()) && this.getClassLoader() == other
        .getClassLoader();
  }

  @Override
  public int hashCode() {
    return this.getName().hashCode();
  }

  private static class ClassLoaderEntry {

    URL url;
    URL codeSource;
    ClassLoader cl;

    public ClassLoaderEntry(URL url, URL codeSource, ClassLoader cl) {
      this.url = url;
      this.codeSource = codeSource;
      this.cl = cl;
    }
  }

  public final boolean isSubclassOf(HierarchyClassInfo other) {
    for (HierarchyClassInfo ci = this; ci != null; ci = ci.getSuperClass()) {
      if (ci.getSuperClass() != null
          && ci.getSuperClass().equals(other)) {
        return true;
      }
    }
    return false;
  }

  public final boolean isAssignableFrom(HierarchyClassInfo other) {
    return (this == other
        || other.isSubclassOf(this)
        || other.implementsInterface(this)
        || (other.isInterface() && getName().equals("java.lang.Object")));
  }

  public final boolean implementsInterface(HierarchyClassInfo other) {
    for (HierarchyClassInfo ci = this; ci != null; ci = ci.getSuperClass()) {
      if (ci.getInterfaces() != null) {
        for (HierarchyClassInfo iface : ci.getInterfaces()) {
          if (iface != null) {
            if (iface.equals(other) || iface.implementsInterface(other)) {
              return true;
            }
          }
        }
      }
    }
    return false;
  }
}
