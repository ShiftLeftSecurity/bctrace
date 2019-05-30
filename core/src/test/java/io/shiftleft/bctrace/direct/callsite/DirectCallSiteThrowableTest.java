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
import static org.junit.Assert.assertTrue;

import io.shiftleft.bctrace.BcTraceTest;
import io.shiftleft.bctrace.TestClass;
import io.shiftleft.bctrace.filter.CallSiteFilter;
import io.shiftleft.bctrace.hierarchy.UnloadedClass;
import io.shiftleft.bctrace.hook.DirectCallSiteHook;
import io.shiftleft.bctrace.hook.Hook;
import io.shiftleft.bctrace.runtime.BctraceRuntimeException;
import io.shiftleft.bctrace.runtime.listener.direct.$io_shiftleft_bctrace_direct_callsite_DirectCallSiteThrowableTest$CallSiteThrowableListener1;
import io.shiftleft.bctrace.runtime.listener.direct.$io_shiftleft_bctrace_direct_callsite_DirectCallSiteThrowableTest$CallSiteThrowableListener2;
import io.shiftleft.bctrace.runtime.listener.direct.$io_shiftleft_bctrace_direct_callsite_DirectCallSiteThrowableTest$CallSiteThrowableListener3;
import io.shiftleft.bctrace.runtime.listener.direct.DirectCallSiteThrowableListener;
import java.lang.reflect.InvocationTargetException;
import org.junit.Test;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * @author Ignacio del Valle Alles idelvall@shiftleft.io
 */
public class DirectCallSiteThrowableTest extends BcTraceTest {

  @Test
  public void testThrowableReported() throws Exception {
    StringBuilder sb = new StringBuilder();
    CallSiteThrowableListener1 callSiteListener1 = new CallSiteThrowableListener1(sb);
    Class clazz = getInstrumentClass(TestClass.class, new Hook[]{
        new DirectCallSiteHook(
            new CallSiteFilter(
                "io/shiftleft/bctrace/TestClass",
                "throwRuntimeException",
                "()V") {
              @Override
              public boolean acceptMethod(UnloadedClass clazz, MethodNode mn, int methodId) {
                return true;
              }
            },
            callSiteListener1)
    }, false);

    try {
      clazz.getMethod("getLongWithConditionalException", boolean.class).invoke(null, true);
    } catch (InvocationTargetException iex) {
      assertEquals(TestClass.RTE_MESSAGE, iex.getTargetException().getMessage());
    }
    assertEquals(TestClass.RTE_MESSAGE, sb.toString());
  }

  public static class CallSiteThrowableListener1 extends DirectCallSiteThrowableListener implements
      $io_shiftleft_bctrace_direct_callsite_DirectCallSiteThrowableTest$CallSiteThrowableListener1 {

    private final StringBuilder sb;

    public CallSiteThrowableListener1(StringBuilder sb) {
      this.sb = sb;
    }

    @ListenerMethod
    public Throwable onAfterCall(Class clazz, Object instance, Object callSiteInstance,
        Throwable th) {
      sb.append(th.getMessage());
      return th;
    }
  }

  @Test
  public void testThrowableReportedAndChanged() throws Exception {
    RuntimeException runtimeException = new RuntimeException();
    CallSiteThrowableListener2 callSiteListener2 = new CallSiteThrowableListener2(runtimeException);
    Class clazz = getInstrumentClass(TestClass.class, new Hook[]{
        new DirectCallSiteHook(
            new CallSiteFilter(
                "io/shiftleft/bctrace/TestClass",
                "throwRuntimeException",
                "()V") {
              @Override
              public boolean acceptMethod(UnloadedClass clazz, MethodNode mn, int methodId) {
                return true;
              }
            },
            callSiteListener2)
    }, false);

    boolean captured = false;
    try {
      clazz.getMethod("getLongWithConditionalException", boolean.class).invoke(null, true);
    } catch (InvocationTargetException iex) {
      assertTrue(iex.getTargetException() == runtimeException);
      captured = true;
    }
    assertTrue(captured);
  }

  public static class CallSiteThrowableListener2 extends DirectCallSiteThrowableListener implements
      $io_shiftleft_bctrace_direct_callsite_DirectCallSiteThrowableTest$CallSiteThrowableListener2 {

    private final RuntimeException runtimeException;

    public CallSiteThrowableListener2(RuntimeException runtimeException) {
      this.runtimeException = runtimeException;
    }

    @ListenerMethod
    public Throwable onAfterCall(Class clazz, Object instance,
        Object callSiteInstance, Throwable th) {
      return runtimeException;
    }
  }

  @Test
  public void testUnexpectedThrowableInListener() throws Exception {
    RuntimeException runtimeException = new RuntimeException("Unexpected!");
    CallSiteThrowableListener3 callSiteListener3 = new CallSiteThrowableListener3(false);
    Class clazz = getInstrumentClass(TestClass.class, new Hook[]{
        new DirectCallSiteHook(
            new CallSiteFilter(
                "io/shiftleft/bctrace/TestClass",
                "throwRuntimeException",
                "()V") {
              @Override
              public boolean acceptMethod(UnloadedClass clazz, MethodNode mn, int methodId) {
                return true;
              }
            },
            callSiteListener3)
    }, false);

    boolean captured = false;
    try {
      clazz.getMethod("getLongWithConditionalException", boolean.class).invoke(null, true);
    } catch (InvocationTargetException iex) {
      if (iex.getTargetException() instanceof RuntimeException &&
          iex.getTargetException().getMessage().equals("Unexpected!")) {
        captured = true;
      }
    }
    assertTrue(captured);
  }

  @Test
  public void testExpectedThrowableInListener() throws Exception {
    RuntimeException runtimeException = new RuntimeException("Unexpected!");
    CallSiteThrowableListener3 callSiteListener3 = new CallSiteThrowableListener3(true);
    Class clazz = getInstrumentClass(TestClass.class, new Hook[]{
        new DirectCallSiteHook(
            new CallSiteFilter(
                "io/shiftleft/bctrace/TestClass",
                "throwRuntimeException",
                "()V") {
              @Override
              public boolean acceptMethod(UnloadedClass clazz, MethodNode mn, int methodId) {
                return true;
              }
            },
            callSiteListener3)
    }, false);

    boolean captured = false;
    try {
      clazz.getMethod("getLongWithConditionalException", boolean.class).invoke(null, true);
    } catch (InvocationTargetException iex) {
      if (iex.getTargetException().getClass() == RuntimeException.class &&
          iex.getTargetException().getMessage().equals("Expected!")) {
        captured = true;
      }
    }
    assertTrue(captured);
  }

  public static class CallSiteThrowableListener3 extends DirectCallSiteThrowableListener implements
      $io_shiftleft_bctrace_direct_callsite_DirectCallSiteThrowableTest$CallSiteThrowableListener3 {

    private final boolean expected;

    public CallSiteThrowableListener3(boolean expected) {
      this.expected = expected;
    }

    @ListenerMethod
    public Throwable onAfterCall(Class clazz, Object instance, Object callSiteInstance,
        Throwable th) {
      if (expected) {
        throw new BctraceRuntimeException(new RuntimeException("Expected!"));
      } else {
        throw new BctraceRuntimeException(new RuntimeException("Unexpected!"));
      }
    }
  }
}