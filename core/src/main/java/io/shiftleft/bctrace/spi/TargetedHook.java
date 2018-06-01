package io.shiftleft.bctrace.spi;

import io.shiftleft.bctrace.impl.TargetedFilter;
import io.shiftleft.bctrace.runtime.listener.specific.DirectListener;

public class TargetedHook extends DynamicHook<TargetedFilter, DirectListener> {

  public TargetedHook(TargetedFilter filter, DirectListener listener) {
    super(filter, listener, filter.getMethodDescriptor());
  }
}
