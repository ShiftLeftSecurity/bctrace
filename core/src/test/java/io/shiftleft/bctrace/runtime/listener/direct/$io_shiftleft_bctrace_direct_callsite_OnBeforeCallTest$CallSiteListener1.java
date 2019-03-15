package io.shiftleft.bctrace.runtime.listener.direct;

/**
 * This accessory interface is needed for testing purposes only. The agent will generate it on
 * CallbackTransformer.class at runtime
 */
public interface $io_shiftleft_bctrace_direct_callsite_OnBeforeCallTest$CallSiteListener1 {

  public void onBeforeCall(Class clazz, Object instance,
      Object callSiteInstance, Object src, int srcPos,
      Object dest, int destPos,
      int length);
}
