package io.shiftleft.bctrace.hook.direct;

import io.shiftleft.bctrace.filter.Filter;
import io.shiftleft.bctrace.hook.Hook;
import io.shiftleft.bctrace.runtime.listener.direct.DirectListener;
import io.shiftleft.bctrace.runtime.listener.direct.DirectListener.DynamicArgsType;
import io.shiftleft.bctrace.runtime.listener.direct.DirectListener.ListenerMethod;
import io.shiftleft.bctrace.runtime.listener.direct.DirectListener.ListenerType;
import java.lang.reflect.Method;
import org.objectweb.asm.Type;

public abstract class DirectHook<F extends Filter, L extends DirectListener> implements
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
          "Invalid required arguments of @ListenerMethod '" + listenerMethod.getDeclaringClass()
              .getName()
              + "." + listenerMethod.getName() + "'");
    }
    if (!checkDynamicArgs(argumentTypes, returnType, listenerMethod.getParameterTypes(), type)) {
      throw new Error(
          "Arguments of @ListenerMethod '" + listenerMethod.getDeclaringClass()
              .getName()
              + "." + listenerMethod.getName()
              + "' don't correspond to the required arguments plus the arguments of the target method to be instrumented");
    }
    checkReturnType(returnType, listenerMethod, type);
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

  private static void checkReturnType(Type returnType, Method listenerMethod, ListenerType type) {
    boolean error = false;
    String requiredType = null;
    Type listenerReturnType = Type.getReturnType(listenerMethod);
    if (type.getDynamicArgsType() == DynamicArgsType.ARGUMENTS) {
      error = !listenerReturnType.getInternalName().equals("V");
      requiredType = "void";
    } else if (type.getDynamicArgsType() == DynamicArgsType.ARGUMENTS_RETURN) {
      error = !returnType.equals(listenerReturnType);
      requiredType = returnType.getClassName();
    } else if (type.getDynamicArgsType() == DynamicArgsType.ARGUMENTS_THROWABLE) {
      error = !listenerReturnType.getInternalName().equals("java/lang/Throwable");
      requiredType = "java.lang.Throwable";
    } else {
      throw new AssertionError();
    }
    if (error) {
      throw new Error("Return type of @ListenerMethod '" + listenerMethod.getDeclaringClass()
          .getName() + "." + listenerMethod.getName() + "' has to be of type " + requiredType);
    }
  }

  private static boolean checkDynamicArgs(Type[] argumentTypes, Type returnType,
      Class[] listenerMethodArgs, ListenerType type) {
    int fixedArgsCount = type.getFixedArgs().length;
    if (type.getDynamicArgsType() == DynamicArgsType.ARGUMENTS) {
      if (fixedArgsCount + argumentTypes.length != listenerMethodArgs.length) {
        return false;
      }
      for (int i = 0; i < argumentTypes.length; i++) {
        if (!argumentTypes[i].equals(Type.getType(listenerMethodArgs[i + fixedArgsCount]))) {
          return false;
        }
      }
      return true;
    } else if (type.getDynamicArgsType() == DynamicArgsType.ARGUMENTS_RETURN) {
      int offset;
      if (returnType.getDescriptor().equals("V")) {
        offset = 0;
      } else {
        if (!returnType
            .equals(Type.getType(listenerMethodArgs[argumentTypes.length + fixedArgsCount]))) {
          return false;
        }
        offset = 1;
      }
      if (fixedArgsCount + argumentTypes.length + offset != listenerMethodArgs.length) {
        return false;
      }
      for (int i = 0; i < argumentTypes.length; i++) {
        if (!argumentTypes[i].equals(Type.getType(listenerMethodArgs[i + fixedArgsCount]))) {
          return false;
        }
      }
      return true;
    } else if (type.getDynamicArgsType() == DynamicArgsType.ARGUMENTS_THROWABLE) {
      if (fixedArgsCount + argumentTypes.length != listenerMethodArgs.length) {
        return false;
      }
      for (int i = 0; i < argumentTypes.length; i++) {
        if (!argumentTypes[i].equals(Type.getType(listenerMethodArgs[i + fixedArgsCount]))) {
          return false;
        }
      }
      return true;
    } else {
      throw new AssertionError();
    }
  }
}
