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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Ignacio del Valle Alles idelvall@shiftleft.io
 */
public final class MethodRegistry {

  private static final MethodRegistry INSTANCE = new MethodRegistry();

  private final ArrayList<MethodInfo> methodArray = new ArrayList<MethodInfo>();
  private final Map<MethodInfo, Integer> methodMap = new HashMap<MethodInfo, Integer>();

  public static MethodRegistry getInstance() {
    return INSTANCE;
  }

  private MethodRegistry() {
  }

  public synchronized MethodInfo getMethod(int id) {
    return methodArray.get(id);
  }

  public synchronized int getMethodId(String binaryClassName, String methodName, String methodDescriptor) {
    MethodInfo mi = new MethodInfo(binaryClassName, methodName, methodDescriptor);
    Integer id = methodMap.get(mi);
    if (id == null) {
      methodArray.add(mi);
      id = methodArray.size() - 1;
      methodMap.put(mi, id);
    }
    return id;
  }

  public synchronized int size() {
    return methodArray.size();
  }
}
