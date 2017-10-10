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
package io.shiftleft.bctrace.asm.util;

import io.shiftleft.bctrace.spi.HierarchyClassInfo;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Ignacio del Valle Alles idelvall@shiftleft.io
 */
public class ClassInfoCache {

  private static final ClassInfoCache INSTANCE = new ClassInfoCache();

  private final Map<ClassLoader, Map<String, HierarchyClassInfo>> maps = new HashMap<ClassLoader, Map<String, HierarchyClassInfo>>();
  private final Map<String, HierarchyClassInfo> bootstrapMap = new HashMap<String, HierarchyClassInfo>();

  private ClassInfoCache() {
  }

  public static ClassInfoCache getInstance() {
    return INSTANCE;
  }

  public synchronized void add(String className, ClassLoader cl, HierarchyClassInfo cn) {
    Map<String, HierarchyClassInfo> map;
    if (cl == null) {
      map = bootstrapMap;
    } else {
      map = maps.get(cl);
      if (map == null) {
        map = new HashMap<String, HierarchyClassInfo>();
        maps.put(cl, map);
      }
    }
    map.put(className, cn);
  }

  public synchronized HierarchyClassInfo get(String className, ClassLoader cl) {
    Map<String, HierarchyClassInfo> map;
    if (cl == null) {
      map = bootstrapMap;
    } else {
      map = maps.get(cl);
      if (map == null) {
        return null;
      }
    }
    return map.get(className);
  }
}
