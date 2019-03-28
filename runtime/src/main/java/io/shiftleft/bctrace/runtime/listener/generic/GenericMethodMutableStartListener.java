package io.shiftleft.bctrace.runtime.listener.generic;

public abstract class GenericMethodMutableStartListener extends GenericMethodListener {

  /**
   * Invoked by instrumented methods before any of its original instructions (if multiple plugins
   * are registered, listener notification is performed according to their respective plugin
   * registration order).
   *
   * Allows changing the arguments of the target method.
   *
   * @param methodId method id (as defined by MethodRegistry)
   * @param clazz class defining the method.
   * @param instance instance where the method is invoked. Null if the method is static
   * @param args arguments passed to the method. returns false;
   * @return The new argument values
   */
  public abstract Object[] onStart(int methodId, Class clazz, Object instance, Object[] args);
}