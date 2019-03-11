package io.shiftleft.bctrace.runtime.listener.generic;

public abstract class GenericMethodStartListener extends GenericMethodListener {

  /**
   * Invoked by instrumented methods before any of its original instructions (if multiple plugins
   * are registered, listener notification is performed according to their respective plugin
   * registration order).
   *
   * @param methodId method id (as defined by MethodRegistry)
   * @param clazz class defining the method.
   * @param instance instance where the method is invoked. Null if the method is static
   * @param args arguments passed to the method. returns false;
   */
  public abstract void onStart(int methodId, Class clazz, Object instance, Object[] args);
}