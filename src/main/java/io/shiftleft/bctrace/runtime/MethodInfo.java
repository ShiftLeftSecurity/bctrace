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

/**
 *
 * @author Ignacio del Valle Alles idelvall@shiftleft.io
 */
public final class MethodInfo implements Comparable<MethodInfo> {

  private final String binaryClassName;
  private final String methodName;
  private final String methodDescriptor;
  private String representation;

  public MethodInfo(String binaryClassName, String methodName, String methodDescriptor) {
    this.binaryClassName = binaryClassName;
    this.methodName = methodName;
    this.methodDescriptor = methodDescriptor;
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

  @Override
  public String toString() {
    // only generated when needed
    if (this.representation == null) {
      this.representation = this.binaryClassName + "." + this.methodName + this.methodDescriptor;
    }
    return this.representation;
  }

  @Override
  public int compareTo(MethodInfo methodInfo) {
    int classCmp = this.binaryClassName.compareTo(methodInfo.binaryClassName);
    if (classCmp != 0) {
      return classCmp;
    }
    int nameCmp = this.methodName.compareTo(methodInfo.methodName);
    if (classCmp != 0) {
      return nameCmp;
    }
    return this.methodDescriptor.compareTo(methodInfo.methodDescriptor);
  }
}
