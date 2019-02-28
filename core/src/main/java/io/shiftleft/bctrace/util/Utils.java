package io.shiftleft.bctrace.util;

import io.shiftleft.bctrace.runtime.listener.direct.DirectListener;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.security.ProtectionDomain;

public final class Utils {

  private static final sun.misc.Unsafe unsafe = getPlatformUnsafe();
  private static final Method UNSAFE_DEFINE_METHOD = getUnsafeDefineMethod();
  private static final Class METHOD_HANDLE_CLASS = getClass("java.lang.invoke.MethodHandles");
  private static final Class LOOKUP_CLASS = getClass("java.lang.invoke.MethodHandles$Lookup");
  private static final Method PRIVATE_LOOKUP_IN_METHOD = getPrivateLookupInMethod(
      METHOD_HANDLE_CLASS, LOOKUP_CLASS);

  private Utils() {
  }

  private static Method getUnsafeDefineMethod() {
    try {
      return sun.misc.Unsafe.class
          .getMethod("defineClass", String.class, byte[].class, int.class, int.class,
              ClassLoader.class, ProtectionDomain.class);
    } catch (NoSuchMethodException ex) {
      return null;
    }
  }

  private static Class getClass(String name) {
    try {
      return Class.forName(name);
    } catch (ClassNotFoundException ex) {
      return null;
    }
  }

  private static Method getPrivateLookupInMethod(Class methodHandlesClass, Class lookupClass) {
    if (methodHandlesClass == null || lookupClass == null) {
      return null;
    }
    try {
      return methodHandlesClass
          .getMethod("privateLookupIn", Class.class, lookupClass);
    } catch (NoSuchMethodException ex) {
      return null;
    }
  }


  private static sun.misc.Unsafe getPlatformUnsafe() {
    try {
      Field[] fields = sun.misc.Unsafe.class.getDeclaredFields();
      Field instanceField = null;
      if (fields != null) {
        for (int i = 0; i < fields.length; i++) {
          Field field = fields[i];
          if (sun.misc.Unsafe.class.isAssignableFrom(field.getType())) {
            instanceField = field;
            break;
          }
        }
      }
      if (instanceField == null) {
        throw new Error("Unable to get platform sun.misc.Unsafe singleton instance");
      }
      instanceField.setAccessible(true);
      return (sun.misc.Unsafe) instanceField.get(null);
    } catch (Exception ex) {
      throw new Error(ex);
    }
  }

  public static byte[] toByteArray(InputStream is) throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    byte[] buffer = new byte[1024];
    while (true) {
      int r = is.read(buffer);
      if (r == -1) {
        break;
      }
      out.write(buffer, 0, r);
    }
    return out.toByteArray();
  }

  public static String getJvmInterfaceNameForDirectListener(String directListenerClassName) {
    return DirectListener.class.getPackage().getName().replace('.', '/') + "/$"
        + directListenerClassName.replace('.', '_').replace('/', '_');
  }

  public static Class defineClass(String name, byte[] bytecode, Class moduleClass) {

    if (UNSAFE_DEFINE_METHOD != null) {
      try {
        Class ret = (Class) UNSAFE_DEFINE_METHOD
            .invoke(unsafe, name, bytecode, 0, bytecode.length, moduleClass.getClassLoader(),
                moduleClass.getClassLoader() == null ? null : moduleClass.getProtectionDomain());
        unsafe.ensureClassInitialized(ret);
        return ret;
      } catch (Exception ex) {
        throw new AssertionError(ex);
      }
    } else if (PRIVATE_LOOKUP_IN_METHOD != null) {
      try {
        Object lookup = METHOD_HANDLE_CLASS.getMethod("lookup").invoke(null);
        Object privateLookup = PRIVATE_LOOKUP_IN_METHOD.invoke(null, moduleClass, lookup);
        return (Class) LOOKUP_CLASS.getMethod("defineClass", byte[].class)
            .invoke(privateLookup, bytecode);
      } catch (Exception ex) {
        throw new AssertionError(ex);
      }
    }
    return null;
  }
}
