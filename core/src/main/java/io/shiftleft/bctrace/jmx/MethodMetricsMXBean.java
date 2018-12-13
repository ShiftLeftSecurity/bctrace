package io.shiftleft.bctrace.jmx;

import io.shiftleft.bctrace.jmx.ClassMetrics.ClassInfo;

public interface MethodMetricsMXBean {

  public boolean isInstrumentedMethodCountersEnabled();

  public void setInstrumentedMethodCountersEnabled(boolean enabled);

  public String viewMethodRegistry();

  public String viewInstrumentedMethodsCallCounters();

  public String viewInstrumentedMethods();
}
