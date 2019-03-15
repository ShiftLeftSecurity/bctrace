package io.shiftleft.bctrace.hook.direct;

import io.shiftleft.bctrace.runtime.listener.direct.CallSiteListener;
import io.shiftleft.bctrace.filter.Filter;

public class CallSiteHook<F extends Filter> extends DirectHook<F, CallSiteListener> {

  public CallSiteHook(F filter, CallSiteListener listener) {
    super(filter, listener, listener.getCallSiteMethodDescriptor());
  }
}