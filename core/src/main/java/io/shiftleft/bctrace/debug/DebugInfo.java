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
package io.shiftleft.bctrace.debug;

import io.shiftleft.bctrace.spi.SystemProperty;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 * @author Ignacio del Valle Alles idelvall@shiftleft.io
 */
public class DebugInfo {

  private static final DebugInfo INSTANCE = new DebugInfo();

  private static final boolean ENABLED = System.getProperty(SystemProperty.DEBUG_SERVER) != null;

  private final Set<ClassInfo> requestedToInstrument = Collections.synchronizedSet(new LinkedHashSet<ClassInfo>());
  private final Set<ClassInfo> instrumentable = Collections.synchronizedSet(new LinkedHashSet<ClassInfo>());
  private final Map<Integer, AtomicInteger> instrumentedMethods = Collections.synchronizedMap(new HashMap<Integer, AtomicInteger>());

  public static DebugInfo getInstance() {
    return INSTANCE;
  }

  public void addRequestedToInstrument(Class clazz) {
    this.requestedToInstrument.add(new ClassInfo(clazz));
  }

  public void addInstrumentable(String className, ClassLoader cl) {
    this.instrumentable.add(new ClassInfo(className, String.valueOf(cl)));
  }

  public void setInstrumented(Integer methodId, boolean instrumented) {
    if (instrumented) {
      instrumentedMethods.put(methodId, new AtomicInteger());
    } else {
      instrumentedMethods.remove(methodId);
    }
  }

  public void increaseCallCounter(Integer methodId) {
    instrumentedMethods.get(methodId).incrementAndGet();
  }

  public Integer getCallCounter(Integer methodId) {
    AtomicInteger counter = instrumentedMethods.get(methodId);
    if (counter == null) {
      return null;
    }
    return counter.get();
  }

  public static boolean isEnabled() {
    return ENABLED;
  }

  public ClassInfo[] getInstrumentable() {
    synchronized (instrumentable) {
      return instrumentable.toArray(new ClassInfo[instrumentable.size()]);
    }
  }

  public ClassInfo[] getRequestedToInstrumentation() {
    synchronized (requestedToInstrument) {
      return requestedToInstrument.toArray(new ClassInfo[requestedToInstrument.size()]);
    }
  }

  public static class ClassInfo {

    private final String className;
    private final String classLoader;

    private ClassInfo(Class clazz) {
      this(clazz.getName(), String.valueOf(clazz.getClassLoader()));
    }

    public ClassInfo(String className, String classLoader) {
      this.className = className;
      this.classLoader = classLoader;
    }

    public String getClassName() {
      return className;
    }

    public String getClassLoader() {
      return classLoader;
    }

    @Override
    public int hashCode() {
      return className.hashCode();
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
      final ClassInfo other = (ClassInfo) obj;
      if ((this.className == null) ? (other.className != null) : !this.className.equals(other.className)) {
        return false;
      }
      if ((this.classLoader == null) ? (other.classLoader != null) : !this.classLoader.equals(other.classLoader)) {
        return false;
      }
      return true;
    }
  }
}
