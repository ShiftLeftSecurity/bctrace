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
package io.shiftleft.bctrace.jmx;

import io.shiftleft.bctrace.Bctrace;
import io.shiftleft.bctrace.logging.Level;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import javax.management.MBeanServer;
import javax.management.ObjectName;

/**
 * @author Ignacio del Valle Alles idelvall@shiftleft.io
 */
public class ClassMetrics implements ClassMetricsMXBean {

  private static final ClassMetrics INSTANCE = new ClassMetrics();

  static {
    try {
      MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
      ObjectName name = new ObjectName("io.shiftleft.bctrace:type=ClassMetrics");
      mbs.registerMBean(INSTANCE, name);
    } catch (Throwable th) {
      Bctrace.getAgentLogger().log(Level.ERROR,
          "Error found while registering bctrace JMX class metrics mBean", th);
    }
  }

  private final Set<ClassInfo> requestedToTransform = Collections
      .synchronizedSet(new LinkedHashSet<ClassInfo>());
  private final Set<ClassInfo> queriedClasses = Collections
      .synchronizedSet(new LinkedHashSet<ClassInfo>());

  private ClassMetrics() {
  }

  public static ClassMetrics getInstance() {
    return INSTANCE;
  }

  public void addRequestedToInstrument(Class clazz) {
    this.requestedToTransform.add(new ClassInfo(clazz));
  }

  public void addInstrumentableClass(String className, ClassLoader cl) {
    this.queriedClasses.add(new ClassInfo(className, String.valueOf(cl)));
  }

  @Override
  public ClassInfo[] getQueriedClasses(String classNameToken) {
    return filter(classNameToken, queriedClasses);
  }

  @Override
  public ClassInfo[] getClassesRequestedToTransform(String classNameToken) {
    return filter(classNameToken, requestedToTransform);
  }

  public ClassInfo[] filter(String classNameToken, Set<ClassInfo> classInfos) {
    classNameToken = classNameToken.replace('.', '/');
    TreeSet<ClassInfo> set = null;
    synchronized (classInfos) {
      for (ClassInfo ci : classInfos) {
        if (classNameToken == null || ci.getClassName().contains(classNameToken)) {
          if (set == null) {
            set = new TreeSet<ClassInfo>();
          }
          set.add(ci);
        }
      }
    }
    if (set == null) {
      return null;
    } else {
      return set.toArray(new ClassInfo[set.size()]);
    }
  }

  public static interface ClassInfoMBean {

    public String getClassName();

    public String getClassLoader();
  }

  public static class ClassInfo implements ClassInfoMBean, Comparable<ClassInfo> {

    private final String className;
    private final String classLoader;

    private ClassInfo(Class clazz) {
      this(clazz.getName(), String.valueOf(clazz.getClassLoader()));
    }

    public ClassInfo(String className, String classLoader) {
      this.className = className.replace('.', '/');
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
      if ((this.className == null) ? (other.className != null)
          : !this.className.equals(other.className)) {
        return false;
      }
      if ((this.classLoader == null) ? (other.classLoader != null)
          : !this.classLoader.equals(other.classLoader)) {
        return false;
      }
      return true;
    }

    @Override
    public int compareTo(ClassInfo o) {
      int ret;
      if (className != null) {
        ret = className.compareTo(o.className);
      } else {
        if (o.className == null) {
          ret = 0;
        } else {
          ret = -1;
        }
      }
      if (ret != 0) {
        return ret;
      }
      if (classLoader != null) {
        ret = classLoader.compareTo(o.classLoader);
      } else {
        if (o.classLoader == null) {
          ret = 0;
        } else {
          ret = -1;
        }
      }
      return ret;
    }
  }
}
