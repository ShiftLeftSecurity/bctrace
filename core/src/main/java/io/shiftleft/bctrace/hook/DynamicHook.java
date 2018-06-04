package io.shiftleft.bctrace.hook;

import io.shiftleft.bctrace.filter.Filter;
import io.shiftleft.bctrace.runtime.listener.specific.DynamicListener;
import io.shiftleft.bctrace.runtime.listener.specific.DynamicListener.ListenerMethod;
import io.shiftleft.bctrace.runtime.listener.specific.DynamicListener.ListenerType;
import java.lang.reflect.Method;
import org.objectweb.asm.Type;

public abstract class DynamicHook<F extends Filter, L extends DynamicListener> implements
    Hook<F, L> {

  private final F filter;
  private final L listener;

  protected DynamicHook(F filter, L listener, String methodDescriptor) {
    this.filter = filter;
    this.listener = listener;
    checkListenerMethod(methodDescriptor);
  }

  @Override
  public final F getFilter() {
    return filter;
  }

  @Override
  public final L getListener() {
    return listener;
  }

  private void checkListenerMethod(String methodDescriptor) {
    Type[] argumentTypes = Type.getArgumentTypes(methodDescriptor);
    Method listenerMethod = this.listener.getListenerMethod();
    ListenerMethod annotation = listenerMethod.getAnnotation(ListenerMethod.class);
    if (annotation == null) {
      return;
    }
    ListenerType type = annotation.type();
    Class[] fixedArgs = type.getFixedArgs();
    Class<?>[] args = listenerMethod.getParameterTypes();

    if (!checkFixedArgs(type.getFixedArgs(), listenerMethod.getParameterTypes())) {
      throw new Error(
          "Invalid required arguments of @ListenerMethod `" + listenerMethod.getDeclaringClass()
              .getName()
              + "." + listenerMethod.getName() + "`");
    }
    if (!checkDynamicArgs(argumentTypes, listenerMethod.getParameterTypes(),
        type.getFixedArgs().length)) {
      throw new Error(
          "Arguments of @ListenerMethod `" + listenerMethod.getDeclaringClass()
              .getName()
              + "." + listenerMethod.getName()
              + "` don't correspond to the required arguments plus the arguments of the target method to be instrumented");
    }
  }

  private static boolean checkFixedArgs(Class[] fixedArgs, Class[] listenerMethodArgs) {
    if (fixedArgs.length > listenerMethodArgs.length) {
      return false;
    }
    for (int i = 0; i < fixedArgs.length; i++) {
      if (!fixedArgs[i].equals(listenerMethodArgs[i])) {
        return false;
      }
    }
    return true;
  }

  private static boolean checkDynamicArgs(Type[] argumentTypes, Class[] listenerMethodArgs,
      int start) {
    if (argumentTypes == null) {
      return start == listenerMethodArgs.length;
    } else {
      if (start + argumentTypes.length != listenerMethodArgs.length) {
        return false;
      }
      for (int i = 0; i < argumentTypes.length; i++) {
        if (!argumentTypes[i].equals(Type.getType(listenerMethodArgs[i + start]))) {
          return false;
        }
      }
      return true;
    }
  }

}
