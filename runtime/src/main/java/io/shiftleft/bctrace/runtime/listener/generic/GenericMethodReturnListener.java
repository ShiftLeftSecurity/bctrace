package io.shiftleft.bctrace.runtime.listener.generic;

public abstract class GenericMethodReturnListener extends GenericMethodListener {

  /**
   * Invoked by instrumented methods just before returning a value.
   *
   * @param methodId method id (as defined by MethodRegistry)
   * @param clazz class defining the method.
   * @param instance instance where the method is invoked. Null if the method is static
   * @param args arguments passed to the method.
   * @param ret Object being returned by the method. Wrapper type if the original return type is
   * primitive. <code>null</code> if the method return type is <code>void</code> or the method
   * raises a throwable
   * @return Object to be returned by the instrumented method  (ignored if target method return type
   * is void)
   */
  public abstract Object onReturn(int methodId, Class clazz, Object instance, Object[] args,
      Object ret);

  /**
   * Declares if returned value must to be passed or not in onFinish() notifications. Default
   * implementation returns <code>true</code>.
   *
   * Override and return <code>false</code> to save unnecessary return value boxing
   */
  public boolean requiresReturnedValue() {
    return true;
  }

}