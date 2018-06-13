package io.shiftleft.bctrace.hook;

import io.shiftleft.bctrace.filter.Filter;
import io.shiftleft.bctrace.runtime.listener.specific.DirectListener;
import io.shiftleft.bctrace.runtime.listener.specific.DirectListener.DynamicArgsType;
import io.shiftleft.bctrace.runtime.listener.specific.DirectListener.ListenerMethod;
import io.shiftleft.bctrace.runtime.listener.specific.DirectListener.ListenerType;
import java.lang.reflect.Method;
import org.objectweb.asm.Type;

public abstract class DirectHook<F extends Filter, L extends DirectListener> extends
    Hook<F, L> {

  private final F filter;
  private final L listener;

  protected DirectHook(F filter, L listener, String methodDescriptor) {
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
    Type returnType = Type.getReturnType(methodDescriptor);
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
    if (!checkDynamicArgs(argumentTypes, returnType, listenerMethod.getParameterTypes(), type)) {
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

  private static boolean checkDynamicArgs(Type[] argumentTypes, Type returnType,
      Class[] listenerMethodArgs,
      ListenerType type) {
    int start = type.getFixedArgs().length;
    if (type.getDynamicArgsType() == DynamicArgsType.ARGUMENTS || returnType.getDescriptor()
        .equals("V")) {
      if (start + argumentTypes.length != listenerMethodArgs.length) {
        return false;
      }
      for (int i = 0; i < argumentTypes.length; i++) {
        if (!argumentTypes[i].equals(Type.getType(listenerMethodArgs[i + start]))) {
          return false;
        }
      }
      return true;
    } else {
      if (start + argumentTypes.length + 1 != listenerMethodArgs.length) {
        return false;
      }
      for (int i = 0; i < argumentTypes.length; i++) {
        if (!argumentTypes[i].equals(Type.getType(listenerMethodArgs[i + start]))) {
          return false;
        }
      }
      if (!returnType.equals(Type.getType(listenerMethodArgs[argumentTypes.length + start]))) {
        return false;
      }
      return true;
    }
  }

}
