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
package io.shiftleft.bctrace;

import io.shiftleft.bctrace.util.collections.IntObjectHashMap;
import io.shiftleft.bctrace.util.collections.ObjectIntHashMap;
import io.shiftleft.bctrace.util.collections.ObjectIntMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Ignacio del Valle Alles idelvall@shiftleft.io
 */
public final class MethodRegistryImpl extends MethodRegistry {

  private AtomicInteger counter = new AtomicInteger(1);
  private final IntObjectHashMap<MethodInfo> map1 = new IntObjectHashMap<MethodInfo>();
  private final ObjectIntHashMap<MethodInfo> map2 = new ObjectIntHashMap<MethodInfo>();

  MethodRegistryImpl() {
  }

  public synchronized MethodInfo getMethod(int methodId) {
    return map1.get(methodId);
  }

  public synchronized int registerMethodId(MethodInfo mi) {
    int methodId = map2.get(mi);
    if (methodId == 0) {
      methodId = counter.getAndIncrement();
      map1.put(methodId, mi);
      map2.put(mi, methodId);
    }
    return methodId;
  }

  public synchronized int getMethodId(MethodInfo mi) {
    return map2.get(mi);
  }

  public synchronized int size() {
    return map1.size();
  }

  public synchronized void remove(int methodId) {
    MethodInfo mi = map1.remove(methodId);
    if (mi != null) {
      map2.remove(mi);
    }
  }
}
