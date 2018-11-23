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
package io.shiftleft.bctrace.direct.method;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import io.shiftleft.bctrace.BcTraceTest;
import io.shiftleft.bctrace.TestClass;
import io.shiftleft.bctrace.filter.MethodFilter;
import io.shiftleft.bctrace.hook.Hook;
import io.shiftleft.bctrace.hook.direct.MethodHook;
import io.shiftleft.bctrace.runtime.listener.direct.DirectListener;
import org.junit.Test;

/**
 * @author Ignacio del Valle Alles idelvall@shiftleft.io
 */
public class OnStartTest extends BcTraceTest {

  @Test
  public void testDirectStart() throws Exception {
    StringBuilder sb = new StringBuilder();
    DirectListener listener1 = new DirectListener1(sb);
    DirectListener listener2 = new DirectListener2(sb);
    Class clazz = getInstrumentClass(TestClass.class, new Hook[]{
        new MethodHook(new MethodFilter("io/shiftleft/bctrace/TestClass", "concatenateStringArrays",
            "([Ljava/lang/String;[Ljava/lang/String;)[Ljava/lang/String;"), listener1),
        new MethodHook(new MethodFilter("io/shiftleft/bctrace/TestClass", "concatenateStringArrays",
            "([Ljava/lang/String;[Ljava/lang/String;)[Ljava/lang/String;"), listener2)
    }, false);

    String[] s1 = {"a", "b"};
    String[] s2 = {"c", "d"};
    clazz.getMethod("concatenateStringArrays", String[].class, String[].class).invoke(null, s1, s2);
    assertEquals("12", sb.toString());
  }

  /**
   * This accessory interface is needed for testing purposes only. The agent will generate it on
   * CallbackTransformer.class at runtime
   */
  public static interface DirectListener1Interface {

    public void onStart(Class clazz, Object instance, String[] array1, String[] array2);
  }

  public static class DirectListener1 extends DirectListener implements DirectListener1Interface {

    private final StringBuilder sb;

    public DirectListener1(StringBuilder sb) {
      this.sb = sb;
    }

    @ListenerMethod(type = ListenerType.onStart)
    public void onStart(Class clazz, Object instance, String[] array1, String[] array2) {
      sb.append("1");
      assertEquals(clazz.getName(), TestClass.class.getName());
      assertNull(instance);
      assertNotNull(array1);
      assertNotNull(array2);
    }
  }

  /**
   * This accessory interface is needed for testing purposes only. The agent will generate it on
   * CallbackTransformer.class at runtime
   */
  public static interface DirectListener2Interface extends DirectListener1Interface {

  }

  public static class DirectListener2 extends DirectListener implements DirectListener2Interface {

    private final StringBuilder sb;

    public DirectListener2(StringBuilder sb) {
      this.sb = sb;
    }

    @ListenerMethod(type = ListenerType.onStart)
    public void onStart(Class clazz, Object instance, String[] array1, String[] array2) {
      sb.append("2");
      assertEquals(clazz.getName(), TestClass.class.getName());
      assertNull(instance);
      assertNotNull(array1);
      assertNotNull(array2);
    }
  }
}
