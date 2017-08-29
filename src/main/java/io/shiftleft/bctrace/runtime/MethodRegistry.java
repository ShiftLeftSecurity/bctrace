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
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 *
 * @author Ignacio del Valle Alles idelvall@shiftleft.io
 */
public final class MethodRegistry {

  private static final MethodRegistry INSTANCE = new MethodRegistry();

  private AtomicInteger counter = new AtomicInteger(0);
  private ConcurrentSkipListMap<MethodInfo,Integer> methodInfoToInt = new ConcurrentSkipListMap<MethodInfo,Integer>();
  private ConcurrentSkipListMap<Integer,MethodInfo> methodIdToInt = new ConcurrentSkipListMap<Integer, MethodInfo>();

  public static MethodRegistry getInstance() {
    return INSTANCE;
  }

  private MethodRegistry() {
  }

  public void reset() {
    this.counter.set(0);
    this.methodInfoToInt.clear();
    this.methodIdToInt.clear();
  }

  public int size() {
    return this.methodIdToInt.size();
  }

  public MethodInfo getMethod(int id) {
    return this.methodIdToInt.get(id);
  }

  public int getMethodId(String binaryClassName, String methodName, String methodDescriptor) {
    MethodInfo methodInfo = new MethodInfo(binaryClassName, methodName, methodDescriptor);
    return this.getMethodId(methodInfo);
  }

  public int getMethodId(MethodInfo methodInfo) {
    Integer arg = this.methodInfoToInt.get(methodInfo);
    if (arg == null) {
      int id = this.counter.getAndAdd(1);
      this.methodInfoToInt.put(methodInfo, id);
      this.methodIdToInt.put(id, methodInfo);
      return id;
    } else {
      return arg;
    }
  }

  public boolean methodIdExists(String binaryClassName, String methodName, String methodDescriptor) {
    MethodInfo methodInfo = new MethodInfo(binaryClassName, methodName, methodDescriptor);
    return this.methodIDExists(methodInfo);
  }

  public boolean methodIDExists(MethodInfo methodInfo) {
    return this.methodInfoToInt.containsKey(methodInfo);
  }
}
