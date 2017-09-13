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
package io.shiftleft.bctrace.runtime;

import io.shiftleft.bctrace.asm.utils.ASMUtils;
import org.objectweb.asm.tree.ClassNode;
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
  private final String representation;

  public static MethodInfo from(ClassNode cn, MethodNode mn) {
    return new MethodInfo(cn.name, mn.name, mn.desc, mn.access);
  }

  public MethodInfo(String binaryClassName, String methodName, String methodDescriptor, int modifiers) {
    this.binaryClassName = binaryClassName;
    this.methodName = methodName;
    this.methodDescriptor = methodDescriptor;
    this.modifiers = modifiers;
    this.representation = binaryClassName + "." + methodName + methodDescriptor;
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

  public boolean isPrivate() {
    return ASMUtils.isPrivate(this.modifiers);
  }

  @Override
  public String toString() {
    return this.representation;
  }

  @Override
  public int hashCode() {
    int hash = 3;
    hash = 79 * hash + (this.representation != null ? this.representation.hashCode() : 0);
    return hash;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    final MethodInfo other = (MethodInfo) obj;
    if ((this.representation == null) ? (other.representation != null) : !this.representation.equals(other.representation)) {
      return false;
    }
    return true;
  }
}
