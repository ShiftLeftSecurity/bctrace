package io.shiftleft.bctrace.runtime.listener.generic;

public abstract class GenericMethodThrowableListener extends GenericMethodListener {

  /**
   * Invoked by instrumented methods just before raising a throwable.
   *
   * @param methodId method id (as defined by MethodRegistry)
   * @param clazz class defining the method.
   * @param instance instance where the method is invoked. Null if the method is static
   * @param args arguments passed to the method.
   * @param th The throwable originally raised by the target method.
   * @return Throwable to be raised
   */
  public abstract Throwable onThrow(int methodId, Class clazz, Object instance, Object[] args,
      Throwable th);
}