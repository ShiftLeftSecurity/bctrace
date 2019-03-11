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

import static org.junit.Assert.assertEquals;

import io.shiftleft.bctrace.BcTraceTest.ByteClassLoader;
import io.shiftleft.bctrace.filter.MethodFilter.DirectMethodFilter;
import io.shiftleft.bctrace.hook.DirectMethodHook;
import io.shiftleft.bctrace.hook.Hook;
import io.shiftleft.bctrace.runtime.listener.direct.$io_shiftleft_bctrace_CallBackTransformerTest$SampleListener1;
import io.shiftleft.bctrace.runtime.listener.direct.$io_shiftleft_bctrace_CallBackTransformerTest$SampleListener2;
import io.shiftleft.bctrace.runtime.listener.direct.DirectMethodStartListener;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import org.junit.Test;

/**
 * @author Ignacio del Valle Alles idelvall@shiftleft.io
 */
public class CallBackTransformerTest {

  public static Class getCallBackClass(final Hook[] hooks) throws Exception {
    ByteClassLoader cl = new ByteClassLoader(hooks, CallBackTransformerTest.class.getClassLoader());
    return cl.loadClass("io.shiftleft.bctrace.runtime.Callback");
  }

  @Test
  public void testDynamic() throws Exception {

    final long aLong = System.currentTimeMillis();
    SampleListener1 listener1 = new SampleListener1();
    SampleListener2 listener2 = new SampleListener2();
    Hook[] hooks = new Hook[]{
        new DirectMethodHook(
            new DirectMethodFilter("io/shiftleft/bctrace/TestClass", "fact", "(J)J"),
            listener1) {
        },
        new DirectMethodHook(new DirectMethodFilter("io/shiftleft/bctrace/TestClass", "factWrapper",
            "(Ljava/lang/Long;)J"),
            listener2)};

    Class callBackClass = getCallBackClass(hooks);
    Field listenersField = callBackClass.getField("listeners");
    listenersField.set(null, new Object[]{listener1, listener2});

    Method[] declaredMethods = callBackClass.getDeclaredMethods();
    for (int i = 0; i < declaredMethods.length; i++) {
      Method m = declaredMethods[i];
      if (m.getName().endsWith("onEvent1")) {
        m.invoke(null, 0, null, null, aLong);
      }
      if (m.getName().endsWith("onEvent2")) {
        m.invoke(null, 1, null, null, new Long(aLong));
      }
    }
    assertEquals(listener1.toString(), Long.toString(aLong));
    assertEquals(listener2.toString(), Long.toString(aLong));
  }

  public static class SampleListener1 extends DirectMethodStartListener implements
      $io_shiftleft_bctrace_CallBackTransformerTest$SampleListener1 {

    final StringBuilder sb = new StringBuilder();

    @ListenerMethod
    public void onEvent1(Class clazz, Object instance, long n) {
      sb.append(n);
    }

    @Override
    public String toString() {
      return sb.toString();
    }
  }

  public static class SampleListener2 extends DirectMethodStartListener implements
      $io_shiftleft_bctrace_CallBackTransformerTest$SampleListener2 {

    final StringBuilder sb = new StringBuilder();

    @ListenerMethod
    public void onEvent2(Class clazz, Object instance, Long n) {
      sb.append(n);
    }

    @Override
    public String toString() {
      return sb.toString();
    }
  }
}
