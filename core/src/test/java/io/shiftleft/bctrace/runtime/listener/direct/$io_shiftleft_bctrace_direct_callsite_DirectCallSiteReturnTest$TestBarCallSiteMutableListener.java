package io.shiftleft.bctrace.runtime.listener.direct;

/**
 * This accessory interface is needed for testing purposes only. The agent will generate it on
 * CallbackTransformer.class at runtime
 */
public interface $io_shiftleft_bctrace_direct_callsite_DirectCallSiteReturnTest$TestBarCallSiteMutableListener {

  public int onAfterCall(Class clazz, Object instance,
      Object callSiteInstance, String s, int ret);
}
