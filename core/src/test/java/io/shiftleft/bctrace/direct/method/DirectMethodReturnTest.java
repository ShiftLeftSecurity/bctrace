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
package io.shiftleft.bctrace.direct.method;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import io.shiftleft.bctrace.BcTraceTest;
import io.shiftleft.bctrace.TestClass;
import io.shiftleft.bctrace.filter.MethodFilter.DirectMethodFilter;
import io.shiftleft.bctrace.hook.DirectMethodHook;
import io.shiftleft.bctrace.hook.Hook;
import io.shiftleft.bctrace.runtime.listener.direct.$io_shiftleft_bctrace_direct_method_DirectMethodReturnTest$DirectListener3;
import io.shiftleft.bctrace.runtime.listener.direct.$io_shiftleft_bctrace_direct_method_DirectMethodReturnTest$DirectListener4;
import io.shiftleft.bctrace.runtime.listener.direct.$io_shiftleft_bctrace_direct_method_DirectMethodReturnTest$DirectListenerVoid;
import io.shiftleft.bctrace.runtime.listener.direct.DirectMethodReturnListener;
import java.util.Arrays;
import org.junit.Test;

/**
 * @author Ignacio del Valle Alles idelvall@shiftleft.io
 */
public class DirectMethodReturnTest extends BcTraceTest {

  @Test
  public void testDirectReturn() throws Exception {
    StringBuilder sb = new StringBuilder();
    DirectListener3 listener3 = new DirectListener3(sb);
    DirectListener4 listener4 = new DirectListener4(sb);
    Class clazz = getInstrumentClass(TestClass.class, new Hook[]{
        new DirectMethodHook(
            new DirectMethodFilter("io/shiftleft/bctrace/TestClass", "concatenateStringArrays",
                "([Ljava/lang/String;[Ljava/lang/String;)[Ljava/lang/String;"), listener3),
        new DirectMethodHook(
            new DirectMethodFilter("io/shiftleft/bctrace/TestClass", "concatenateStringArrays",
                "([Ljava/lang/String;[Ljava/lang/String;)[Ljava/lang/String;"), listener4)
    }, false);

    String[] s1 = {"a", "b"};
    String[] s2 = {"c", "d"};
    String[] ret = (String[]) clazz
        .getMethod("concatenateStringArrays", String[].class, String[].class).invoke(null, s1, s2);
    assertEquals(Arrays.toString(ret), Arrays.toString(new String[]{"1", "2", "3", "4"}));
    assertEquals("21", sb.toString());
  }

  @Test
  public void testDirectReturnVoid() throws Exception {
    StringBuilder sb = new StringBuilder();
    DirectListenerVoid listener = new DirectListenerVoid(sb);
    Class clazz = getInstrumentClass(TestClass.class, new Hook[]{
        new DirectMethodHook(new DirectMethodFilter("io/shiftleft/bctrace/TestClass", "doFrames",
            "()V"), listener)
    }, false);
    clazz.getMethod("doFrames").invoke(null);
    assertEquals("1", sb.toString());
  }

  public static class DirectListener3 extends DirectMethodReturnListener implements
      $io_shiftleft_bctrace_direct_method_DirectMethodReturnTest$DirectListener3 {

    private final StringBuilder sb;

    public DirectListener3(StringBuilder sb) {
      this.sb = sb;
    }

    @ListenerMethod
    public String[] onFinish(Class clazz, Object instance, String[] array1,
        String[] array2, String[] ret) {
      sb.append("1");
      assertEquals(clazz.getName(), TestClass.class.getName());
      assertNull(instance);
      assertNotNull(array1);
      assertNotNull(array2);
      assertTrue(array1.length + array2.length == ret.length);

      return ret;
    }
  }

  public static class DirectListener4 extends DirectMethodReturnListener implements
      $io_shiftleft_bctrace_direct_method_DirectMethodReturnTest$DirectListener4 {

    private final StringBuilder sb;

    public DirectListener4(StringBuilder sb) {
      this.sb = sb;
    }

    @ListenerMethod
    public String[] onFinish(Class clazz, Object instance, String[] array1,
        String[] array2,
        String[] ret) {
      sb.append("2");
      assertEquals(clazz.getName(), TestClass.class.getName());
      assertNull(instance);
      assertNotNull(array1);
      assertNotNull(array2);
      assertTrue(array1.length + array2.length == ret.length);
      return new String[]{"1", "2", "3", "4"};
    }
  }

  public static class DirectListenerVoid extends DirectMethodReturnListener implements
      $io_shiftleft_bctrace_direct_method_DirectMethodReturnTest$DirectListenerVoid {

    private final StringBuilder sb;

    public DirectListenerVoid(StringBuilder sb) {
      this.sb = sb;
    }

    @ListenerMethod
    public void onFinish(Class clazz, Object instance) {
      sb.append("1");
      assertEquals(clazz.getName(), TestClass.class.getName());
      assertNull(instance);
    }
  }
}
