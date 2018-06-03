package io.shiftleft.bctrace.hook;

import io.shiftleft.bctrace.runtime.listener.specific.CallSiteListener;
import io.shiftleft.bctrace.filter.Filter;

public abstract class CallSiteHook<F extends Filter> extends DynamicHook<F, CallSiteListener> {

  public CallSiteHook(F filter, CallSiteListener listener) {
    super(filter, listener, listener.getCallSiteMethodDescriptor());
  }
}