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
 * of this source code, which includes information that is confidential and/or proprietary, and
 * is a trade secret, of ShiftLeft, Inc.
 *
 * ANY REPRODUCTION, MODIFICATION, DISTRIBUTION, PUBLIC PERFORMANCE, OR PUBLIC DISPLAY
 * OF OR THROUGH USE OF THIS SOURCE CODE WITHOUT THE EXPRESS WRITTEN CONSENT OF ShiftLeft, Inc.
 * IS STRICTLY PROHIBITED, AND IN VIOLATION OF APPLICABLE LAWS AND INTERNATIONAL TREATIES.
 * THE RECEIPT OR POSSESSION OF THIS SOURCE CODE AND/OR RELATED INFORMATION DOES NOT
 * CONVEY OR IMPLY ANY RIGHTS TO REPRODUCE, DISCLOSE OR DISTRIBUTE ITS
 * CONTENTS, OR TO MANUFACTURE, USE, OR SELL ANYTHING THAT IT MAY DESCRIBE, IN WHOLE OR IN PART.
 */
package io.shiftleft.bctrace;

import io.shiftleft.bctrace.asm.CallbackTransformer;
import io.shiftleft.bctrace.asm.Transformer;
import io.shiftleft.bctrace.asm.util.ASMUtils;
import io.shiftleft.bctrace.hook.Hook;
import io.shiftleft.bctrace.util.Utils;
import java.io.InputStream;

/**
 * @author Ignacio del Valle Alles idelvall@shiftleft.io
 */
public abstract class BcTraceTest {

  public static Bctrace init(ByteClassLoader cl, final Hook[] hooks) throws Exception {
    Agent agent = new Agent() {
      @Override
      public void init(Bctrace bctrace) {
      }

      @Override
      public void afterRegistration() {
      }

      @Override
      public void showMenu() {
      }

      @Override
      public Hook[] getHooks() {
        return hooks;
      }
    };
    Bctrace bctrace = new Bctrace(null, agent, false);
    bctrace.init();
    Object[] listeners = new Object[hooks.length];
    for (int i = 0; i < listeners.length; i++) {
      listeners[i] = hooks[i].getListener();
    }
    Class callBackclass = cl.loadClass("io.shiftleft.bctrace.runtime.Callback");

    callBackclass.getField("listeners").set(null, listeners);
    return bctrace;
  }

  public static Class getInstrumentClass(Class clazz, final Hook[] hooks) throws Exception {
    return getInstrumentClass(clazz, hooks, false);
  }

  public static Class getInstrumentClass(Class clazz, final Hook[] hooks, boolean trace)
      throws Exception {
    ByteClassLoader cl = new ByteClassLoader(hooks, clazz.getClassLoader());
    Bctrace bctrace = init(cl, hooks);
    Transformer transformer = new Transformer(new InstrumentationImpl(null),
        bctrace, null);
    String className = clazz.getCanonicalName();
    String resourceName = className.replace('.', '/') + ".class";
    InputStream is = clazz.getClassLoader().getResourceAsStream(resourceName);
    byte[] bytes = Utils.toByteArray(is);
    byte[] newBytes = transformer.transform(null, className.replace('.', '/'), clazz, null, bytes);
    if (trace) {
      ASMUtils.viewByteCode(newBytes);
    }
    return cl.loadClass(className, newBytes);
  }

  public static class ByteClassLoader extends ClassLoader {

    private final CallbackTransformer callbackTransformer;
    private final Hook[] hooks;

    public ByteClassLoader(Hook[] hooks, ClassLoader parentClassLoader) {
      super(parentClassLoader);
      this.hooks = hooks;
      this.callbackTransformer = new CallbackTransformer(hooks);
    }

    public Class<?> loadClass(String name, byte[] byteCode) {
      return super.defineClass(name, byteCode, 0, byteCode.length);
    }

    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
      if (name.equals("io.shiftleft.bctrace.runtime.Callback")) {
        try {
          String resourceName = name.replace('.', '/') + ".class";
          InputStream is = CallBackTransformerTest.class.getClassLoader()
              .getResourceAsStream(resourceName);
          byte[] bytes = Utils.toByteArray(is);
          byte[] newBytes = this.callbackTransformer
              .transform(null, name.replace('.', '/'), null, null, bytes);
          if (newBytes != null) {
            bytes = newBytes;
          }
          return loadClass(name, bytes);
        } catch (Exception ex) {
          throw new RuntimeException(ex);
        }
      } else {
        return super.loadClass(name, resolve);
      }
    }
  }
}
