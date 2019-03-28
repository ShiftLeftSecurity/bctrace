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
package io.shiftleft.bctrace.filter;

import java.util.HashSet;
import java.util.Set;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

public abstract class CallSiteFilter extends MethodFilter {

  protected final String callSiteType;
  protected final String callSiteMethodName;
  protected final String callSiteMethodDescriptor;
  protected final Set<Integer> lineNumberSet;

  public CallSiteFilter(String callSiteType, String callSiteMethodName,
      String callSiteMethodDescriptor) {
    this(callSiteType, callSiteMethodName, callSiteMethodDescriptor, null);
  }

  public CallSiteFilter(String callSiteType, String callSiteMethodName,
      String callSiteMethodDescriptor, int[] lineNumbers) {
    if (callSiteMethodDescriptor == null) {
      throw new NullPointerException("methodDescriptor");
    }
    this.callSiteMethodDescriptor = callSiteMethodDescriptor;
    this.callSiteMethodName = callSiteMethodName;
    this.callSiteType = callSiteType;
    if (lineNumbers != null) {
      lineNumberSet = new HashSet<Integer>();
      for (int i = 0; i < lineNumbers.length; i++) {
        lineNumberSet.add(lineNumbers[i]);
      }
    } else {
      lineNumberSet = null;
    }
  }

  /**
   * Returns a boolean that determines whether to instrument the specified call site. This
   * implementation returns true if call site descriptor matches the one specified in the
   * constructor, and if specified, it also checks if the call site type and call site method name
   * match, as well as the line number is in the array provided in the contructor (if any).
   *
   * This method can be overwritten.
   *
   * @param lineNumber 0 if not available
   */
  public boolean acceptCallSite(ClassNode cn, MethodNode mn,
      MethodInsnNode callSite, int lineNumber) {
    boolean ret = callSiteMethodDescriptor.equals(callSite.desc);
    if (this.callSiteType != null) {
      ret = ret && callSiteType.equals(callSite.owner);
    }
    if (this.callSiteMethodName != null) {
      ret = ret && callSiteMethodName.equals(callSite.name);
    }
    if (lineNumber >= 0 && this.lineNumberSet != null) {
      ret = ret && this.lineNumberSet.contains(lineNumber);
    }
    return ret;
  }

  public final String getMethodDescriptor() {
    return callSiteMethodDescriptor;
  }
}
