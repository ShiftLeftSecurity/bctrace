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

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.WeakHashMap;

/**
 * @author Ignacio del Valle Alles idelvall@shiftleft.io
 */
public final class MethodRegistry {

  private static final MethodRegistry INSTANCE = new MethodRegistry();

  private final ReferenceQueue<ClassLoader> referenceQueue = new ReferenceQueue<ClassLoader>();

  // Free ids available for reuse, due to classloader garbage collection
  private final ArrayList<Integer> freeSlots = new ArrayList<Integer>();
  // Id to MethodInfo mapping. Allows direct access by id (incremental);
  private final ArrayList<MethodInfo> methodArray = new ArrayList<MethodInfo>();
  // MethodInfo to id mapping.
  private final Map<MethodInfo, Integer> defaultMethodMap = new HashMap<MethodInfo, Integer>();
  private final Map<ClassLoader, Map<MethodInfo, Integer>> classLoaderMethodMaps = new WeakHashMap<ClassLoader, Map<MethodInfo, Integer>>();

  public static MethodRegistry getInstance() {
    return INSTANCE;
  }

  private MethodRegistry() {
  }

  public synchronized MethodInfo getMethod(Integer id) {
    return methodArray.get(id);
  }

  public synchronized Integer registerMethodId(MethodInfo mi, ClassLoader cl) {
    expunge();
    Map<MethodInfo, Integer> methodMap = getOrCreateMethodMap(cl);
    Integer id = methodMap.get(mi);
    if (id == null) {
      if (freeSlots.size() > 0) {
        id = freeSlots.remove(0);
        methodArray.set(id, mi);
      } else {
        methodArray.add(mi);
        id = methodArray.size() - 1;
      }
      methodMap.put(mi, id);
    }
    return id;
  }

  private synchronized Map<MethodInfo, Integer> getOrCreateMethodMap(ClassLoader cl) {
    if (cl == null) {
      return defaultMethodMap;
    } else {
      Map<MethodInfo, Integer> methodMap = classLoaderMethodMaps.get(cl);
      if (methodMap == null) {
        methodMap = new HashMap<MethodInfo, Integer>();
        classLoaderMethodMaps.put(cl, methodMap);
      }
      return methodMap;
    }
  }

  private synchronized Map<MethodInfo, Integer> getMethodMap(ClassLoader cl) {
    if (cl == null) {
      return defaultMethodMap;
    } else {
      return classLoaderMethodMaps.get(cl);
    }
  }


  public synchronized List<Integer> getMethodId(MethodInfo mi) {
    List<Integer> ret = null;
    Integer id = defaultMethodMap.get(mi);
    if (id != null) {
      if (ret == null) {
        ret = new ArrayList<Integer>();
      }
      ret.add(id);
    }
    for (Map<MethodInfo, Integer> mm : classLoaderMethodMaps.values()) {
      id = defaultMethodMap.get(mi);
      if (id != null) {
        if (ret == null) {
          ret = new ArrayList<Integer>();
        }
        ret.add(id);
      }
    }
    return ret;
  }

  public synchronized Integer getMethodId(MethodInfo mi, ClassLoader cl) {
    Map<MethodInfo, Integer> mm = getMethodMap(cl);
    if (mm == null) {
      return null;
    }
    return mm.get(mi);
  }

  public synchronized ClassLoader getClassLoader(Integer id) {
    MethodInfo mi = methodArray.get(id);
    if (defaultMethodMap.containsKey(mi)) {
      return null;
    }
    for (Entry<ClassLoader, Map<MethodInfo, Integer>> entry : classLoaderMethodMaps.entrySet()) {
      if (entry.getValue().containsKey(mi)) {
        return entry.getKey();
      }
    }
    throw new IllegalArgumentException("Unknown method id '" + id + "'");
  }

  public synchronized int size() {
    return methodArray.size() - freeSlots.size();
  }

  private void expunge() {
    Reference<? extends ClassLoader> ref;
    while ((ref = referenceQueue.poll()) != null) {
      this.expunge(ref.get());
    }
  }

  private void expunge(ClassLoader cl) {
    if (cl == null) {
      return;
    }
    Map<MethodInfo, Integer> mm = classLoaderMethodMaps.remove(cl);
    for (Integer id : mm.values()) {
      methodArray.set(id, null);
      freeSlots.add(id);
    }
  }
}
