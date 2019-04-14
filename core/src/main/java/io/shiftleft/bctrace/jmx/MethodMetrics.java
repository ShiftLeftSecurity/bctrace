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
import io.shiftleft.bctrace.MethodInfo;
import io.shiftleft.bctrace.MethodRegistry;
import io.shiftleft.bctrace.jmx.ClassMetrics.ClassInfo;
import io.shiftleft.bctrace.jmx.ClassMetrics.ClassInfoMBean;
import io.shiftleft.bctrace.logging.Level;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import javax.management.MBeanServer;
import javax.management.ObjectName;

/**
 * @author Ignacio del Valle Alles idelvall@shiftleft.io
 */
public class MethodMetrics implements MethodMetricsMXBean {

  private static final MethodMetrics INSTANCE = new MethodMetrics();

  static {
    try {
      MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
      ObjectName name = new ObjectName("io.shiftleft.bctrace:type=MethodMetrics");
      mbs.registerMBean(INSTANCE, name);
    } catch (Throwable th) {
      Bctrace.getAgentLogger().log(Level.ERROR,
          "Error found while registering bctrace JMX method metrics mBean", th);
    }
  }

  private volatile Map<Integer, AtomicInteger> instrumentedMethodCallCounters;
  private final Set<Integer> instrumentedMethodIds = new TreeSet<Integer>();

  private MethodMetrics() {
  }

  public static MethodMetrics getInstance() {
    return INSTANCE;
  }

  public synchronized void reportInstrumented(Integer methodId) {
    instrumentedMethodIds.add(methodId);
  }

  public Integer getCallCount(int methodId) {
    if (instrumentedMethodCallCounters != null) {
      synchronized (this) {
        if (instrumentedMethodCallCounters != null) {
          AtomicInteger counter = instrumentedMethodCallCounters.get(methodId);
          if (counter == null) {
            return null;
          }
          return counter.get();
        }
      }
    }
    return null;
  }

  public void incrementCallCounter(int methodId) {
    if (instrumentedMethodCallCounters != null) {
      synchronized (this) {
        if (instrumentedMethodCallCounters != null) {
          AtomicInteger counter = instrumentedMethodCallCounters.get(methodId);
          if (counter == null) {
            counter = new AtomicInteger();
            instrumentedMethodCallCounters.put(methodId, counter);
          }
          counter.incrementAndGet();
        }
      }
    }
  }

  @Override
  public boolean isInstrumentedMethodCountersEnabled() {
    return instrumentedMethodCallCounters != null;
  }

  @Override
  public synchronized void setInstrumentedMethodCountersEnabled(boolean enabled) {
    if (enabled == false) {
      instrumentedMethodCallCounters = null;
    } else if (instrumentedMethodCallCounters == null) {
      instrumentedMethodCallCounters = new HashMap<Integer, AtomicInteger>();
    }
  }

  @Override
  public synchronized String viewMethodRegistry() {
    StringBuilder sb = new StringBuilder();
    MethodRegistry mr = MethodRegistry.getInstance();
    sb.append("# id").append("\t").append("class").append("\t").append("method");
    sb.append("\n");
    for (int i = 0; i < mr.size(); i++) {
      MethodInfo mi = mr.getMethod(i);
      sb.append(i).append("\t").append(mi.getBinaryClassName()).append("\t")
          .append(mi.getMethodName()).append(mi.getMethodDescriptor());
      sb.append("\n");
    }
    return sb.toString();
  }

  @Override
  public synchronized String viewInstrumentedMethodsCallCounters() {
    if (instrumentedMethodCallCounters == null) {
      throw new IllegalStateException(
          "Call counters are disabled. Run setInstrumentedMethodCountersEnabled(true) to enable them");
    }
    StringBuilder sb = new StringBuilder();
    sb.append("# id").append("\t").append("class").append("\t").append("method").append("\t")
        .append("calls");
    sb.append("\n");
    for (Entry<Integer, AtomicInteger> entry : instrumentedMethodCallCounters.entrySet()) {
      MethodInfo mi = MethodRegistry.getInstance().getMethod(entry.getKey());
      sb.append(entry.getKey()).append("\t").append(mi.getBinaryClassName()).append("\t")
          .append(mi.getMethodName()).append(mi.getMethodDescriptor()).append("\t")
          .append(entry.getValue().get());
      sb.append("\n");
    }
    return sb.toString();
  }

  @Override
  public String viewInstrumentedMethods() {
    StringBuilder sb = new StringBuilder();
    sb.append("# id").append("\t").append("class").append("\t").append("method").append("\t");
    sb.append("\n");
    for (Integer methodId : instrumentedMethodIds) {
      MethodInfo mi = MethodRegistry.getInstance().getMethod(methodId);
      sb.append(methodId).append("\t").append(mi.getBinaryClassName()).append("\t")
          .append(mi.getMethodName()).append(mi.getMethodDescriptor());
      sb.append("\n");
    }
    return sb.toString();
  }
}
