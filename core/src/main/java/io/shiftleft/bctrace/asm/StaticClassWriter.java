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
import io.shiftleft.bctrace.spi.hierarchy.HierarchyClassInfo;
import java.io.IOException;
import java.io.InputStream;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/**
 * A {@link ClassWriter} that looks for static class data in the classpath when
 * the classes are not available at runtime, using the HierarchyClassInfo tree.
 *
 * @author Ignacio del Valle Alles idelvall@shiftleft.io
 */
class StaticClassWriter extends ClassWriter {

  /* The classloader that we use to look for the unloaded class */
  private final ClassLoader classLoader;

  /**
   * {@inheritDoc}
   *
   * @param classLoader the class loader that loaded this class
   */
  public StaticClassWriter(
      ClassReader classReader, int flags, ClassLoader classLoader) {
    super(classReader, flags);
    this.classLoader = classLoader;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected String getCommonSuperClass(
      final String type1, final String type2) {
    HierarchyClassInfo ci1 = HierarchyClassInfo.from(type1.replace('/', '.'), classLoader);
    HierarchyClassInfo ci2 = HierarchyClassInfo.from(type2.replace('/', '.'), classLoader);
    if (ci1.isAssignableFrom(ci2)) {
      return type1;
    }
    if (ci2.isAssignableFrom(ci1)) {
      return type2;
    }
    if (ci1.isInterface() || ci2.isInterface()) {
      return "java/lang/Object";
    }

    do {
      // Should never be null, because if ci1 were the Object class
      // or an interface, it would have been caught above.
      ci1 = ci1.getSuperClass();
    } while (!ci1.isAssignableFrom(ci2));
    return ci1.getJVMName();
  }
}
