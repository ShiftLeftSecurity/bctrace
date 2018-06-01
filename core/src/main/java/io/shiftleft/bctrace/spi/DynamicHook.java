package io.shiftleft.bctrace.spi;

import io.shiftleft.bctrace.runtime.listener.Listener;
import io.shiftleft.bctrace.runtime.listener.specific.DirectListener.ListenerMethod;
import io.shiftleft.bctrace.runtime.listener.specific.DirectListener.ListenerType;
import java.lang.reflect.Method;
import org.objectweb.asm.Type;

public class DynamicHook<F extends Filter, L extends Listener> implements Hook<F, L> {

  private final F filter;
  private final L listener;

  public DynamicHook(F filter, L listener, String methodDescriptor) {
    this.filter = filter;
    this.listener = listener;
    checkIntegrity(methodDescriptor);
  }

  @Override
  public final F getFilter() {
    return filter;
  }

  @Override
  public final L getListener() {
    return listener;
  }

  private void checkIntegrity(String methodDescriptor) {
    Method[] declaredMethods = listener.getClass().getDeclaredMethods();
    if (declaredMethods == null || declaredMethods.length == 0) {
      throw new Error("Listener does not define any method " + listener.getClass());
    }
    Type[] argumentTypes = Type.getArgumentTypes(methodDescriptor);
    for (int i = 0; i < declaredMethods.length; i++) {
      checkListenerMethod(argumentTypes, declaredMethods[i]);
    }
  }

  private void checkListenerMethod(Type[] argumentTypes, Method listenerMethod) {
    ListenerMethod annotation = listenerMethod.getAnnotation(ListenerMethod.class);
    if (annotation == null) {
      return;
    }
    ListenerType type = annotation.type();
    Class[] fixedArgs = type.getFixedArgs();
    Class<?>[] args = listenerMethod.getParameterTypes();

    if (!checkFixedArgs(type.getFixedArgs(), listenerMethod.getParameterTypes())) {
      throw new Error(
          "Invalid required arguments in listener method " + listenerMethod.getDeclaringClass()
              .getName()
              + "." + listenerMethod.getName());
    }
    if (!checkDynamicArgs(argumentTypes, listenerMethod.getParameterTypes(),
        type.getFixedArgs().length)) {
      throw new Error(
          "Dynamic arguments in listener method " + listenerMethod.getDeclaringClass()
              .getName()
              + "." + listenerMethod.getName()
              + " don't match the arguments of target method specified in the filter " + filter
              .getClass().getName());
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
