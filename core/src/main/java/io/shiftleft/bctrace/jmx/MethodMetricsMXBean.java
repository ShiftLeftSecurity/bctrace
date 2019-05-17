package io.shiftleft.bctrace.jmx;

public interface MethodMetricsMXBean {

  public boolean isInstrumentedMethodCountersEnabled();

  public void setInstrumentedMethodCountersEnabled(boolean enabled);

  public String viewMethodRegistry();

  public String viewInstrumentedMethodsCallCounters();

}
