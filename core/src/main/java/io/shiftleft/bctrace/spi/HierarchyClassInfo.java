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
public final class HierarchyClassInfo {

  private final ClassNode cn;
  private final ClassLoader cl;

  public HierarchyClassInfo(ClassNode cn, ClassLoader cl) {
    this.cn = cn;
    this.cl = cl;
  }

  public ClassNode getRawClassNode() {
    return cn;
  }

  public HierarchyClassInfo getSuperclass() {
    return getParentClassInfo(this.cn.superName, this.cl);
  }

  public HierarchyClassInfo[] getInterfaces() {
    List<String> interfaces = this.cn.interfaces;
    HierarchyClassInfo[] ret = new HierarchyClassInfo[interfaces.size()];
    int i = 0;
    for (String interfaceName : interfaces) {
      ret[i] = getParentClassInfo(interfaceName, cl);
      i++;
    }
    return ret;
  }

  private static HierarchyClassInfo getParentClassInfo(String name, ClassLoader cl) {
    if (name == null) {
      return null;
    }
    ClassLoaderEntry entry = readClassResource(name + ".class", cl);
    if (entry == null) {
      Bctrace.getInstance().getAgentLogger()
          .warning("Could not get bytecode for hierarchy inspection of class " + name);
      return null;
    }
    HierarchyClassInfo ci = ClassInfoCache.getInstance().get(name, entry.cl);
    if (ci == null) {
      ci = new HierarchyClassInfo(createClassNode(entry.is), entry.cl);
      ClassInfoCache.getInstance().add(name, entry.cl, ci);
    }
    return ci;
  }

  private static ClassLoaderEntry readClassResource(String classResource, ClassLoader loader) {
    if (loader == null) {
      InputStream is = ClassLoader.getSystemResourceAsStream(classResource);
      if (is == null) {
        return null;
      } else {
        return new ClassLoaderEntry(is, loader);
      }
    } else {
      ClassLoaderEntry entry = readClassResource(classResource, loader.getParent());
      if (entry != null) {
        return entry;
      }
      InputStream is = loader.getResourceAsStream(classResource);
      if (is == null) {
        return null;
      } else {
        return new ClassLoaderEntry(is, loader);
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
