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

import io.shiftleft.bctrace.BcTraceTest;
import io.shiftleft.bctrace.TestClass;
import io.shiftleft.bctrace.filter.MethodFilter.DirectMethodFilter;
import io.shiftleft.bctrace.hook.DirectMethodHook;
import io.shiftleft.bctrace.hook.Hook;
import io.shiftleft.bctrace.runtime.BctraceRuntimeException;
import io.shiftleft.bctrace.runtime.listener.direct.*;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import org.junit.Test;

/**
 * @author Ignacio del Valle Alles idelvall@shiftleft.io
 */
public class DirectMethodStartTest extends BcTraceTest {

  @Test
  public void test() throws Exception {
    StringBuilder steps = new StringBuilder();
    DirectListener1 listener1 = new DirectListener1(steps);
    DirectListener2 listener2 = new DirectListener2(steps);
    Class clazz = getInstrumentClass(TestClass.class, new Hook[]{
        new DirectMethodHook(
            new DirectMethodFilter(
                "io/shiftleft/bctrace/TestClass",
                "concatenateStringArrays",
                "([Ljava/lang/String;[Ljava/lang/String;)[Ljava/lang/String;"),
            listener1),
        new DirectMethodHook(
            new DirectMethodFilter(
                "io/shiftleft/bctrace/TestClass",
                "concatenateStringArrays",
                "([Ljava/lang/String;[Ljava/lang/String;)[Ljava/lang/String;"),
            listener2)
    }, false);

    String[] s1 = {"a", "b"};
    String[] s2 = {"c", "d"};
    clazz.getMethod("concatenateStringArrays", String[].class, String[].class).invoke(null, s1, s2);
    assertEquals("12", steps.toString());
  }

  @Test
  public void testChangeArgument() throws Exception {
    StringBuilder steps = new StringBuilder();
    ChangeArgumentListener listener1 = new ChangeArgumentListener(steps, 0);
    ChangeArgumentListener listener2 = new ChangeArgumentListener(steps, 1);
    DirectMethodFilter filter = new DirectMethodFilter(
        "io/shiftleft/bctrace/TestClass",
        "concatenateStringArrays",
        "([Ljava/lang/String;[Ljava/lang/String;)[Ljava/lang/String;");
    Class clazz = getInstrumentClass(TestClass.class, new Hook[]{
        new DirectMethodHook(filter, listener1),
        new DirectMethodHook(filter, listener2)
    }, false);

    String[] s1 = {"a", "b"};
    String[] s2 = {"c", "d"};
    String[] ret = (String[]) clazz
        .getMethod("concatenateStringArrays", String[].class, String[].class).invoke(null, s1, s2);
    assertEquals("11", steps.toString());
    assertEquals(Arrays.toString(new String[]{"a0", "b0", "c1", "d1"}), Arrays.toString(ret));
  }

  @Test
  public void testUnexpectedListenerException() throws Exception {
    StringBuilder steps = new StringBuilder();
    ExceptionListener listener = new ExceptionListener(steps, false);
    DirectMethodFilter filter = new DirectMethodFilter(
        "io/shiftleft/bctrace/TestClass",
        "concatenateStringArrays",
        "([Ljava/lang/String;[Ljava/lang/String;)[Ljava/lang/String;");
    Class clazz = getInstrumentClass(TestClass.class, new Hook[]{
        new DirectMethodHook(filter, listener)
    }, false);
    String[] s1 = {"a", "b"};
    String[] s2 = {"c", "d"};
    String[] ret = (String[]) clazz
        .getMethod("concatenateStringArrays", String[].class, String[].class).invoke(null, s1, s2);
    assertEquals("1", steps.toString());
    assertEquals(Arrays.toString(new String[]{"a", "b", "c", "d"}), Arrays.toString(ret));
  }

  @Test
  public void testExpectedListenerException() throws Exception {
    StringBuilder steps = new StringBuilder();
    ExceptionListener listener = new ExceptionListener(steps, true);
    DirectMethodFilter filter = new DirectMethodFilter(
        "io/shiftleft/bctrace/TestClass",
        "concatenateStringArrays",
        "([Ljava/lang/String;[Ljava/lang/String;)[Ljava/lang/String;");
    Class clazz = getInstrumentClass(TestClass.class, new Hook[]{
        new DirectMethodHook(filter, listener)
    }, false);

    try {
      String[] s1 = {"a", "b"};
      String[] s2 = {"c", "d"};
      String[] ret = (String[]) clazz
          .getMethod("concatenateStringArrays", String[].class, String[].class).invoke(null, s1, s2);
    } catch (InvocationTargetException ite) {
      if (ite.getTargetException().getClass() == RuntimeException.class &&
          ite.getTargetException().getMessage().equals("Expected!")) {
        steps.append("2");
      }
    }
    assertEquals("12", steps.toString());
  }

  public static class DirectListener1 extends DirectMethodStartListener implements
      $io_shiftleft_bctrace_direct_method_DirectMethodStartTest$DirectListener1 {

    private final StringBuilder steps;

    public DirectListener1(StringBuilder steps) {
      this.steps = steps;
    }

    @ListenerMethod
    public void onStart(Class clazz, Object instance, String[] array1, String[] array2) {
      steps.append("1");
      assertEquals(clazz.getName(), TestClass.class.getName());
      assertNull(instance);
      assertNotNull(array1);
      assertNotNull(array2);
    }
  }

  public static class DirectListener2 extends DirectMethodStartListener implements
      $io_shiftleft_bctrace_direct_method_DirectMethodStartTest$DirectListener2 {

    private final StringBuilder steps;

    public DirectListener2(StringBuilder steps) {
      this.steps = steps;
    }

    @ListenerMethod
    public void onStart(Class clazz, Object instance, String[] array1, String[] array2) {
      steps.append("2");
      assertEquals(clazz.getName(), TestClass.class.getName());
      assertNull(instance);
      assertNotNull(array1);
      assertNotNull(array2);
    }
  }

  public static class ChangeArgumentListener extends DirectMethodStartListener implements
      $io_shiftleft_bctrace_direct_method_DirectMethodStartTest$ChangeArgumentListener {

    private final StringBuilder steps;
    private final int argument;

    public ChangeArgumentListener(StringBuilder steps, int argument) {
      this.steps = steps;
      this.argument = argument;
    }

    @Override
    public int getMutableArgumentIndex() {
      return argument;
    }

    @ListenerMethod
    public String[] onStart(Class clazz, Object instance, String[] array1, String[] array2) {
      steps.append("1");
      assertEquals(clazz.getName(), TestClass.class.getName());
      assertNull(instance);
      assertNotNull(array1);
      assertNotNull(array2);
      String[] mutatedArg;
      if (argument == 0) {
        mutatedArg = array1;
      } else {
        mutatedArg = array2;
      }
      String[] ret = new String[mutatedArg.length];
      for (int i = 0; i < ret.length; i++) {
        ret[i] = mutatedArg[i] + argument;
      }
      return ret;
    }
  }

  public static class ExceptionListener extends DirectMethodStartListener implements
      $io_shiftleft_bctrace_direct_method_DirectMethodStartTest$ExceptionListener {

    private final StringBuilder steps;
    private final boolean expected;

    public ExceptionListener(StringBuilder steps, boolean expected) {
      this.steps = steps;
      this.expected = expected;
    }

    @ListenerMethod
    public void onStart(Class clazz, Object instance, String[] array1, String[] array2) {
      steps.append("1");
      if (expected) {
        throw new BctraceRuntimeException(new RuntimeException("Expected!"));
      } else {
        throw new RuntimeException("Unexpected!");
      }
    }
  }
}
