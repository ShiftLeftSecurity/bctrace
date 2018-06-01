package io.shiftleft.bctrace.spi;

import io.shiftleft.bctrace.runtime.listener.specific.CallSiteListener;

public abstract class CallSiteHook<F extends Filter> extends DynamicHook<F, CallSiteListener> {

  public CallSiteHook(F filter, CallSiteListener listener) {
    super(filter, listener, listener.getCallSiteMethodDescriptor());
  }
}