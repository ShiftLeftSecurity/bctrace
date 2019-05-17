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
import io.shiftleft.bctrace.MethodRegistry;
import io.shiftleft.bctrace.MethodRegistry.MethodInfo;
import io.shiftleft.bctrace.logging.Level;
import io.shiftleft.bctrace.util.collections.IntObjectHashMap;
import io.shiftleft.bctrace.util.collections.IntObjectHashMap.EntryVisitor;
import java.lang.management.ManagementFactory;
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

  private volatile IntObjectHashMap<AtomicInteger> callCounters;

  private MethodMetrics() {
  }

  public static MethodMetrics getInstance() {
    return INSTANCE;
  }

  public Integer getCallCount(int methodId) {
    if (callCounters != null) {
      synchronized (this) {
        if (callCounters != null) {
          AtomicInteger counter = callCounters.get(methodId);
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
    if (callCounters != null) {
      synchronized (this) {
        if (callCounters != null) {
          AtomicInteger counter = callCounters.get(methodId);
          if (counter == null) {
            counter = new AtomicInteger();
            callCounters.put(methodId, counter);
          }
          counter.incrementAndGet();
        }
      }
    }
  }

  @Override
  public boolean isInstrumentedMethodCountersEnabled() {
    return callCounters != null;
  }

  @Override
  public synchronized void setInstrumentedMethodCountersEnabled(boolean enabled) {
    if (enabled == false) {
      callCounters = null;
    } else if (callCounters == null) {
      callCounters = new IntObjectHashMap<AtomicInteger>();
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
      sb.append(i).append("\t").append(mi.getJvmClassName()).append("\t")
          .append(mi.getMethodName()).append(mi.getMethodDescriptor());
      sb.append("\n");
    }
    return sb.toString();
  }

  @Override
  public synchronized String viewInstrumentedMethodsCallCounters() {
    if (callCounters == null) {
      throw new IllegalStateException(
          "Call counters are disabled. Run setInstrumentedMethodCountersEnabled(true) to enable them");
    }
    final StringBuilder sb = new StringBuilder();
    sb.append("# id").append("\t").append("class").append("\t").append("method").append("\t")
        .append("calls");
    sb.append("\n");
    final MethodRegistry methodRegistry = MethodRegistry.getInstance();
    callCounters.visitEntries(new EntryVisitor<AtomicInteger>() {
      @Override
      public boolean remove(int key, AtomicInteger value) {
        MethodInfo mi = MethodRegistry.getInstance().getMethod(key);
        sb.append(key).append("\t").append(mi.getJvmClassName()).append("\t")
            .append(mi.getMethodName()).append(mi.getMethodDescriptor()).append("\t")
            .append(value.get());
        sb.append("\n");
        return false;
      }
    });
    return sb.toString();
  }
}
