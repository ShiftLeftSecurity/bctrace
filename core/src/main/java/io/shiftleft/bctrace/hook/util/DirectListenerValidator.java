package io.shiftleft.bctrace.hook.util;

import io.shiftleft.bctrace.runtime.listener.direct.DirectCallSiteReturnListener;
import io.shiftleft.bctrace.runtime.listener.direct.DirectCallSiteStartListener;
import io.shiftleft.bctrace.runtime.listener.direct.DirectCallSiteThrowableListener;
import io.shiftleft.bctrace.runtime.listener.direct.DirectListener;
import io.shiftleft.bctrace.runtime.listener.direct.DirectMethodReturnListener;
import io.shiftleft.bctrace.runtime.listener.direct.DirectMethodStartListener;
import io.shiftleft.bctrace.runtime.listener.direct.DirectMethodThrowableListener;
import org.objectweb.asm.Type;

public final class DirectListenerValidator {

  private static final Type RETURN_VOID = Type.getType("V");

  public static void checkListenerMethod(String filterMethodDescriptor,
      DirectListener directListener) {
    Type[] filterArgumentTypes = Type.getArgumentTypes(filterMethodDescriptor);
    Type filterReturnType = Type.getReturnType(filterMethodDescriptor);
    if (directListener instanceof DirectCallSiteStartListener) {
      checkArgument(directListener, 0, Class.class);
      checkArgument(directListener, 1, Object.class);
      checkArgument(directListener, 2, Object.class);
      checkArguments(directListener, 3, filterArgumentTypes);
      DirectCallSiteStartListener startListener = (DirectCallSiteStartListener) directListener;
      int mutableIndex = startListener.getMutableArgumentIndex();
      if (mutableIndex >= filterArgumentTypes.length) {
        throwError(directListener, "mutableArgumentIndex is out of bounds");
      } else if (mutableIndex >= 0) {
        checkReturnType(directListener, filterArgumentTypes[mutableIndex]);
      }
      checkNumberOfArguments(directListener, 3 + filterArgumentTypes.length);
    } else if (directListener instanceof DirectCallSiteReturnListener) {
      checkArgument(directListener, 0, Class.class);
      checkArgument(directListener, 1, Object.class);
      checkArgument(directListener, 2, Object.class);
      checkArguments(directListener, 3, filterArgumentTypes);
      if (!filterReturnType.equals(RETURN_VOID)) {
        checkArgument(directListener, 3 + filterArgumentTypes.length, filterReturnType);
        checkNumberOfArguments(directListener, 4 + filterArgumentTypes.length);
      } else {
        checkNumberOfArguments(directListener, 3 + filterArgumentTypes.length);
      }
      checkReturnType(directListener, filterReturnType);
    } else if (directListener instanceof DirectCallSiteThrowableListener) {
      checkArgument(directListener, 0, Class.class);
      checkArgument(directListener, 1, Object.class);
      checkArgument(directListener, 2, Object.class);
      checkArguments(directListener, 3, filterArgumentTypes);
      checkArgument(directListener, 3 + filterArgumentTypes.length, Throwable.class);
      checkReturnType(directListener, Throwable.class);
      checkNumberOfArguments(directListener, 4 + filterArgumentTypes.length);


    } else if (directListener instanceof DirectMethodStartListener) {
      checkArgument(directListener, 0, Class.class);
      checkArgument(directListener, 1, Object.class);
      checkArguments(directListener, 2, filterArgumentTypes);
      DirectMethodStartListener startListener = (DirectMethodStartListener) directListener;
      int mutableIndex = startListener.getMutableArgumentIndex();
      if (mutableIndex >= filterArgumentTypes.length) {
        throwError(directListener, "mutableArgumentIndex is out of bounds");
      } else if (mutableIndex >= 0) {
        checkReturnType(directListener, filterArgumentTypes[mutableIndex]);
      } else {
        checkReturnType(directListener, Type.getType("V"));
      }
      checkNumberOfArguments(directListener, 2 + filterArgumentTypes.length);
    } else if (directListener instanceof DirectMethodReturnListener) {
      checkArgument(directListener, 0, Class.class);
      checkArgument(directListener, 1, Object.class);
      checkArguments(directListener, 2, filterArgumentTypes);
      if (!filterReturnType.equals(RETURN_VOID)) {
        checkArgument(directListener, 2 + filterArgumentTypes.length, filterReturnType);
        checkNumberOfArguments(directListener, 3 + filterArgumentTypes.length);
      } else {
        checkNumberOfArguments(directListener, 2 + filterArgumentTypes.length);
      }
      checkReturnType(directListener, filterReturnType);
    } else if (directListener instanceof DirectMethodThrowableListener) {
      checkArgument(directListener, 0, Class.class);
      checkArgument(directListener, 1, Object.class);
      checkArguments(directListener, 2, filterArgumentTypes);
      checkArgument(directListener, 2 + filterArgumentTypes.length, Throwable.class);
      checkReturnType(directListener, Throwable.class);
      checkNumberOfArguments(directListener, 3 + filterArgumentTypes.length);
    } else {
      throw new Error("Unsupported DirectListener type:'" + directListener.getClass() + "'");
    }
  }

  private static void checkReturnType(DirectListener directListener, Class expected) {
    checkReturnType(directListener, Type.getType(expected));
  }

  private static void checkReturnType(DirectListener directListener, Type expected) {
    Class returnType = directListener.getListenerMethod().getReturnType();
    if (!Type.getType(returnType).equals(expected)) {
      throwError(directListener, "Return type should be of type '" + expected.getClassName() + "'");
    }
  }

  private static void checkArgument(DirectListener directListener, int i, Class expected) {
    checkArgument(directListener, i, Type.getType(expected));
  }

  private static void checkArgument(DirectListener directListener, int i, Type expected) {
    Class[] argumentTypes = directListener.getListenerMethod().getParameterTypes();
    if (argumentTypes.length <= i || !Type.getType(argumentTypes[i]).equals(expected)) {
      throwError(directListener,
          "Argument " + i + " is expected to be of type: '" + expected.getClassName() + "'");
    }
  }

  private static void checkArguments(DirectListener directListener, int i,
      Type[] filterArgumentTypes) {
    for (int j = 0; j < filterArgumentTypes.length; j++) {
      checkArgument(directListener, i + j, filterArgumentTypes[j]);
    }
  }

  private static void checkNumberOfArguments(DirectListener directListener, int n) {
    if (directListener.getListenerMethod().getParameterTypes().length != n) {
      throwError(directListener, "Expected number of arguments is " + n);
    }
  }

  private static void throwError(DirectListener directListener, String message) {
    throw new Error(
        "Invalid @ListenerMethod of '" + directListener.getClass() + "': " + message);
  }


}
