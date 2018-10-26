package io.shiftleft.bctrace.runtime.listener.generic;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

public abstract class GenericListener {

  private boolean[] disabledParameters;

  GenericListener() {
  }

  protected abstract Method getListenerSuperMethod();

  protected final Method getListenerMethod() {
    Method superMethod = getListenerSuperMethod();
    try {
      return getClass().getMethod(superMethod.getName(), superMethod.getParameterTypes());
    } catch (NoSuchMethodException ex) {
      throw new AssertionError();
    }
  }

  protected final boolean[] getDisabledArguments() {
    if (disabledParameters == null) {
      Method method = getListenerMethod();
      Annotation[][] parameterAnnotations = method.getParameterAnnotations();
      this.disabledParameters = new boolean[parameterAnnotations.length];
      for (int i = 0; i < parameterAnnotations.length; i++) {
        for (int j = 0; j < parameterAnnotations[i].length; j++) {
          if (parameterAnnotations[i][j].annotationType() == Disabled.class) {
            this.disabledParameters[i] = true;
            break;
          }
        }
      }
    }
    return this.disabledParameters;
  }

  public final boolean requiresMethodId() {
    return !getDisabledArguments()[0];
  }

  public final boolean requiresClass() {
    return !getDisabledArguments()[1];
  }

  public final boolean requiresInstance() {
    return !getDisabledArguments()[2];
  }

  public final boolean requiresArguments() {
    return !getDisabledArguments()[3];
  }
}
