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
package io.shiftleft.bctrace.direct.callsite;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import io.shiftleft.bctrace.BcTraceTest;
import io.shiftleft.bctrace.TestClass;
import io.shiftleft.bctrace.filter.CallSiteFilter;
import io.shiftleft.bctrace.hierarchy.UnloadedClass;
import io.shiftleft.bctrace.hook.DirectCallSiteHook;
import io.shiftleft.bctrace.hook.Hook;
import io.shiftleft.bctrace.runtime.BctraceRuntimeException;
import io.shiftleft.bctrace.runtime.listener.direct.$io_shiftleft_bctrace_direct_callsite_DirectCallSiteStartTest$ArrayCopyListener;
import io.shiftleft.bctrace.runtime.listener.direct.$io_shiftleft_bctrace_direct_callsite_DirectCallSiteStartTest$ChangeArgumentListener;
import io.shiftleft.bctrace.runtime.listener.direct.$io_shiftleft_bctrace_direct_callsite_DirectCallSiteStartTest$ExceptionListener;
import io.shiftleft.bctrace.runtime.listener.direct.$io_shiftleft_bctrace_direct_callsite_DirectCallSiteStartTest$StartListener;
import io.shiftleft.bctrace.runtime.listener.direct.DirectCallSiteStartListener;
import java.lang.reflect.InvocationTargetException;
import org.junit.Test;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * @author Ignacio del Valle Alles idelvall@shiftleft.io
 */
public class DirectCallSiteStartTest extends BcTraceTest {

  @Test
  public void test() throws Exception {
    StringBuilder steps = new StringBuilder();
    ArrayCopyListener arrayCopyListener1 = new ArrayCopyListener(steps, "1");
    ArrayCopyListener arrayCopyListener2 = new ArrayCopyListener(steps, "2");
    CallSiteFilter arrayCopyFilter = new CallSiteFilter(
        "java/lang/System",
        "arraycopy",
        "(Ljava/lang/Object;ILjava/lang/Object;II)V") {
      @Override
      public boolean acceptMethod(UnloadedClass clazz, MethodNode mn) {
        return true;
      }
    };
    Class clazz = getInstrumentClass(TestClass.class, new Hook[]{
        new DirectCallSiteHook(arrayCopyFilter, arrayCopyListener1),
        new DirectCallSiteHook(arrayCopyFilter, arrayCopyListener2)
    }, false);

    clazz.getMethod("arrayCopyWrapper2").invoke(null);
    assertEquals("12", steps.toString());
  }

  @Test
  public void testUnexpectedListenerException() throws Exception {
    StringBuilder steps = new StringBuilder();
    ExceptionListener listener = new ExceptionListener(steps, false);
    CallSiteFilter filter = new CallSiteFilter(
        "io/shiftleft/bctrace/TestClass",
        "getUpperCase",
        "(Ljava/lang/String;)Ljava/lang/String;") {
      @Override
      public boolean acceptMethod(UnloadedClass clazz, MethodNode mn) {
        return true;
      }
    };
    Class clazz = getInstrumentClass(
        TestClass.class,
        new Hook[]{new DirectCallSiteHook(filter, listener)},
        false);

    String greet = (String) clazz.getMethod("greet").invoke(null);
    assertEquals("HELLO WORLD !", greet.toString());
    assertEquals("111", steps.toString());
  }

  @Test
  public void testExpectedListenerException() throws Exception {
    StringBuilder steps = new StringBuilder();
    ExceptionListener listener = new ExceptionListener(steps, true);
    CallSiteFilter filter = new CallSiteFilter(
        "io/shiftleft/bctrace/TestClass",
        "getUpperCase",
        "(Ljava/lang/String;)Ljava/lang/String;") {
      @Override
      public boolean acceptMethod(UnloadedClass clazz, MethodNode mn) {
        return true;
      }
    };
    Class clazz = getInstrumentClass(
        TestClass.class,
        new Hook[]{new DirectCallSiteHook(filter, listener)},
        false);
    try {
      clazz.getMethod("greet").invoke(null);
    } catch (InvocationTargetException ite) {
      if (ite.getTargetException().getClass() == RuntimeException.class &&
          ite.getTargetException().getMessage().equals("Expected!")) {
        steps.append("2");
      }
    }
    assertEquals("12", steps.toString());
  }

  public static class ArrayCopyListener extends DirectCallSiteStartListener implements
      $io_shiftleft_bctrace_direct_callsite_DirectCallSiteStartTest$ArrayCopyListener {

    private final StringBuilder steps;
    private final String message;

    public ArrayCopyListener(StringBuilder steps, String message) {
      this.steps = steps;
      this.message = message;
    }

    @ListenerMethod
    public void onBeforeCall(Class clazz, Object instance,
        Object callSiteInstance, Object src, int srcPos,
        Object dest, int destPos,
        int length) {
      steps.append(message);
      assertEquals(clazz.getName(), TestClass.class.getName());
      assertEquals(instance.getClass().getName(), TestClass.class.getName());
      assertTrue(callSiteInstance == null);
      assertNotNull(src);
      assertNotNull(dest);
    }
  }

  @Test
  public void testChangeArgument() throws Exception {
    StringBuilder steps = new StringBuilder();
    ChangeArgumentListener listener = new ChangeArgumentListener(steps, "bctrace");
    CallSiteFilter filter = new CallSiteFilter(
        "io/shiftleft/bctrace/TestClass",
        "getUpperCase",
        "(Ljava/lang/String;)Ljava/lang/String;",
        new int[]{156}) {
      @Override
      public boolean acceptMethod(UnloadedClass clazz, MethodNode mn) {
        return true;
      }
    };
    Class clazz = getInstrumentClass(
        TestClass.class,
        new Hook[]{new DirectCallSiteHook(filter, listener)},
        false);

    String greet = (String) clazz.getMethod("greet").invoke(null);
    assertEquals("HELLO BCTRACE !", greet.toString());
  }

  public static class ChangeArgumentListener extends DirectCallSiteStartListener implements
      $io_shiftleft_bctrace_direct_callsite_DirectCallSiteStartTest$ChangeArgumentListener {

    private final StringBuilder steps;
    private final String newStr;

    public ChangeArgumentListener(StringBuilder steps, String newStr) {
      this.steps = steps;
      this.newStr = newStr;
    }

    @Override
    public int getMutableArgumentIndex() {
      return 0;
    }

    @ListenerMethod
    public String onStart(Class clazz, Object instance, Object callSiteInstance, String str) {
      steps.append("1");
      return newStr;
    }
  }

  /**
   * Instruments only call sites in lines 152 and 154 (skipping line 153)
   */
  @Test
  public void testLineNumberFiltering() throws Exception {
    StringBuilder steps = new StringBuilder();
    StartListener startListener = new StartListener(steps);
    CallSiteFilter filter = new CallSiteFilter(
        "io/shiftleft/bctrace/TestClass",
        "getUpperCase",
        "(Ljava/lang/String;)Ljava/lang/String;",
        new int[]{154, 158}) {
      @Override
      public boolean acceptMethod(UnloadedClass clazz, MethodNode mn) {
        return true;
      }
    };
    Class clazz = getInstrumentClass(
        TestClass.class,
        new Hook[]{new DirectCallSiteHook(filter, startListener)},
        false);

    clazz.getMethod("greet").invoke(null);
    assertEquals("hello!", steps.toString());
  }

  public static class StartListener extends DirectCallSiteStartListener implements
      $io_shiftleft_bctrace_direct_callsite_DirectCallSiteStartTest$StartListener {

    private final StringBuilder steps;

    public StartListener(StringBuilder steps) {
      this.steps = steps;
    }

    @ListenerMethod
    public void onStart(Class clazz, Object instance, Object callSiteInstance, String message) {
      steps.append(message);
      assertEquals(clazz.getName(), TestClass.class.getName());
      assertTrue(instance == null);
      assertTrue(callSiteInstance == null);
    }
  }

  public static class ExceptionListener extends DirectCallSiteStartListener implements
      $io_shiftleft_bctrace_direct_callsite_DirectCallSiteStartTest$ExceptionListener {

    private final StringBuilder steps;
    private final boolean expected;

    public ExceptionListener(StringBuilder steps, boolean expected) {
      this.steps = steps;
      this.expected = expected;
    }

    @ListenerMethod
    public void onStart(Class clazz, Object instance, Object callSiteInstance, String message) {
      steps.append("1");
      if (expected) {
        throw new BctraceRuntimeException(new RuntimeException("Expected!"));
      } else {
        throw new RuntimeException("Unexpected!");
      }
    }
  }
}
