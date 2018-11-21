/*
 * ShiftLeft, Inc. CONFIDENTIAL
 * Unpublished Copyright (c) 2017 ShiftLeft, Inc., All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains the property of ShiftLeft, Inc.
 * The intellectual and technical concepts contained herein are proprietary to ShiftLeft, Inc.
 * and may be covered by U.S. and Foreign Patents, patents in process, and are protected by
 * trade secret or copyright law. Dissemination of this information or reproduction of this
 * material is strictly forbidden unless prior written permission is obtained
 * from ShiftLeft, Inc. Access to the source code contained herein is hereby forbidden to
 * anyone except current ShiftLeft, Inc. employees, managers or contractors who have executed
 * Confidentiality and Non-disclosure agreements explicitly covering such access.
 *
 * The copyright notice above does not evidence any actual or intended publication or disclosure
 * of this source code, which includeas information that is confidential and/or proprietary, and
 * is a trade secret, of ShiftLeft, Inc.
 *
 * ANY REPRODUCTION, MODIFICATION, DISTRIBUTION, PUBLIC PERFORMANCE, OR PUBLIC DISPLAY
 * OF OR THROUGH USE OF THIS SOURCE CODE WITHOUT THE EXPRESS WRITTEN CONSENT OF ShiftLeft, Inc.
 * IS STRICTLY PROHIBITED, AND IN VIOLATION OF APPLICABLE LAWS AND INTERNATIONAL TREATIES.
 * THE RECEIPT OR POSSESSION OF THIS SOURCE CODE AND/OR RELATED INFORMATION DOES NOT
 * CONVEY OR IMPLY ANY RIGHTS TO REPRODUCE, DISCLOSE OR DISTRIBUTE ITS
 * CONTENTS, OR TO MANUFACTURE, USE, OR SELL ANYTHING THAT IT MAY DESCRIBE, IN WHOLE OR IN PART.
 */
package io.shiftleft.bctrace.runtime.listener.direct;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;

/**
 * DirectListener instances are notified directly from the instrumented method without wrapping or
 * boxing arguments. They are used for high performance notification.
 *
 * DirectListeners do not define a fixed interface. They are used in TargetedHooks, and they must
 * define a single method (with name defined in the ListenerType enum) whose signature matches the
 * signature of the TargetedFilter of the hook in the following way:
 *
 * Suppose the instrumented method defined by the filter being foo(Integer arg1, String arg2), then
 * a valid direct listener method for receiving start events would be:
 *
 * @author Ignacio del Valle Alles idelvall@shiftleft.io
 * @ListenerMethod public void onStart(Class clazz, Object instance, Integer arg1, String arg2)
 */
public abstract class DirectListener {

  private final Method listenerMethod;
  private final ListenerType type;

  public DirectListener() {
    this.listenerMethod = searchListenerMethod();
    this.type = this.listenerMethod.getAnnotation(ListenerMethod.class).type();
  }

  private Method searchListenerMethod() {
    Method ret = null;
    Class clazz = getClass();
    while (clazz != null) {
      Method[] declaredMethods = clazz.getDeclaredMethods();
      for (int j = 0; j < declaredMethods.length; j++) {
        if (declaredMethods[j].getAnnotation(ListenerMethod.class) != null) {
          if (ret != null) {
            if (ret.getName().equals(declaredMethods[j].getName()) &&
                Arrays.equals(ret.getParameterTypes(), declaredMethods[j].getParameterTypes())) {
              continue;
            }
            throw new Error("Only one @ListenerMethod method is allowed in " + getClass());
          }
          ret = declaredMethods[j];
        }
      }
      clazz = clazz.getSuperclass();
    }
    if (ret == null) {
      throw new Error("Listener does not define any @ListenerMethod method " + getClass());
    }
    checkMethodSingature(ret);
    return ret;
  }

  /**
   * Checks that all parameters and return type are loaded by the bootstrap classloader
   */
  private static void checkMethodSingature(Method m) {
    Class<?>[] parameterTypes = m.getParameterTypes();
    for (Class paramClass : parameterTypes) {
      if (!isLoadedByBootstrapClassLoader(paramClass)) {
        throw new Error("Invalid @ListenerMethod parameter of type '" + paramClass.getName()
            + "'. Only types loaded by bootstrap class loader are supported");
      }
    }
    if (!isLoadedByBootstrapClassLoader(m.getReturnType())) {
      throw new Error("Invalid @ListenerMethod return type '" + m.getReturnType().getName()
          + "'. Only types loaded by bootstrap class loader are supported");
    }
  }

  private static boolean isLoadedByBootstrapClassLoader(Class clazz) {
    return clazz.getClassLoader() == null;
  }


  public final Method getListenerMethod() {
    return listenerMethod;
  }

  public final ListenerType getType() {
    return type;
  }

  public static enum DynamicArgsType {
    ARGUMENTS_RETURN,
    ARGUMENTS_THROWABLE,
    ARGUMENTS
  }

  /**
   * Defines the valid names for the listener method, and its fixed first arguments
   */
  public static enum ListenerType {

    // Instrumented class, instrumented instance
    onStart(DynamicArgsType.ARGUMENTS, Class.class, Object.class),
    // Instrumented class, instrumented instance
    onFinish(DynamicArgsType.ARGUMENTS_RETURN, Class.class, Object.class),
    // Instrumented class, instrumented instance, throwable raised
    onFinishThrowable(DynamicArgsType.ARGUMENTS_THROWABLE, Class.class, Object.class,
        Throwable.class),
    // Instrumented (caller) class, instrumented instance, callee instance
    onBeforeCall(DynamicArgsType.ARGUMENTS, Class.class, Object.class, Object.class),
    // Instrumented (caller) class, instrumented instance, callee instance
    onAfterCall(DynamicArgsType.ARGUMENTS_RETURN, Class.class, Object.class, Object.class),
    // Instrumented (caller) class, instrumented instance, callee instance,  throwable raised
    onAfterCallThrowable(DynamicArgsType.ARGUMENTS_THROWABLE, Class.class, Object.class, Object.class,
        Throwable.class);


    private Class[] fixedArgs;
    private DynamicArgsType dynamicArgsType;

    ListenerType(DynamicArgsType dynamicArgsType, Class... fixedArgs) {
      this.fixedArgs = fixedArgs;
      this.dynamicArgsType = dynamicArgsType;
    }

    public Class[] getFixedArgs() {
      return fixedArgs;
    }

    public DynamicArgsType getDynamicArgsType() {
      return dynamicArgsType;
    }
  }

  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.METHOD)
  public static @interface ListenerMethod {

    public ListenerType type();
  }
}
