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

import io.shiftleft.bctrace.asm.util.ASMUtils;
import org.objectweb.asm.tree.MethodNode;

/**
 *
 * @author Ignacio del Valle Alles idelvall@shiftleft.io
 */
public final class MethodInfo {

  private final String binaryClassName;
  private final String methodName;
  private final String methodDescriptor;
  private final int modifiers;

  private String packageName;

  public static MethodInfo from(String binaryClassName, MethodNode mn) {
    return new MethodInfo(binaryClassName, mn.name, mn.desc, mn.access);
  }

  public MethodInfo(String binaryClassName, String methodName, String methodDescriptor, int modifiers) {
    this.binaryClassName = binaryClassName;
    this.methodName = methodName;
    this.methodDescriptor = methodDescriptor;
    this.modifiers = modifiers;
  }

  public String getBinaryClassName() {
    return this.binaryClassName;
  }

  public String getMethodName() {
    return this.methodName;
  }

  public String getMethodDescriptor() {
    return this.methodDescriptor;
  }

  public int getModifiers() {
    return modifiers;
  }

  public String getPackageName() {
    if (packageName == null) {
      packageName = this.binaryClassName.substring(0, this.binaryClassName.lastIndexOf("/")).replace('/', '.');
    }
    return this.packageName;
  }

  public boolean isAbstract() {
    return ASMUtils.isAbstract(this.modifiers);
  }

  public boolean isNative() {
    return ASMUtils.isNative(this.modifiers);
  }

  public boolean isStatic() {
    return ASMUtils.isStatic(this.modifiers);
  }

  public boolean isPublic() {
    return ASMUtils.isPublic(this.modifiers);
  }

  public boolean isProtected() {
    return ASMUtils.isProtected(this.modifiers);
  }

  public boolean isDefault() {
    return !isPublic() && !isProtected() && !isPrivate();
  }

  public boolean isPrivate() {
    return ASMUtils.isPrivate(this.modifiers);
  }

  @Override
  public String toString() {
    return binaryClassName + "." + methodName + methodDescriptor;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    MethodInfo that = (MethodInfo) o;

    if (!binaryClassName.equals(that.binaryClassName)) {
      return false;
    }
    if (!methodName.equals(that.methodName)) {
      return false;
    }
    return methodDescriptor.equals(that.methodDescriptor);
  }

  @Override
  public int hashCode() {
    int result = binaryClassName.hashCode();
    result = 31 * result + methodName.hashCode();
    result = 31 * result + methodDescriptor.hashCode();
    return result;
  }
}
