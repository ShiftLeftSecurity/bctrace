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

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

/**
 * A {@link ClassWriter} that looks for static class data in the classpath when the classes are not
 * available at runtime, using the HierarchyClassInfo tree.
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
   * This method is used when merging stack map frames (if we add branching statements) in order to
   * recompute them (COMPUTE_FRAMES). This API is flawed, since at runtime you dont necessarily have
   * means for knowing the class hierarchy of a type (due to lazy loading). By the time being we
   * just removed features adding branching (like conditional overwriting the return value of a
   * method or the try-catch listener) that we are not actually needing in the agent.
   *
   * So now we just want to be sure that our assumption about this feature not being used holds,
   * hence that implementation. The problem is solvable btw, the API should provide means for only
   * adding needed information and reusing already known stack map frames. This could be a cool
   * contribution to ASM
   *
   * http://chrononsystems.com/blog/java-7-design-flaw-leads-to-huge-backward-step-for-the-jvm
   */
  @Override
  protected String getCommonSuperClass(
      final String type1, final String type2) {

    throw new AssertionError();
  }
}
