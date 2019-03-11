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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import io.shiftleft.bctrace.BcTraceTest;
import io.shiftleft.bctrace.TestClass;
import io.shiftleft.bctrace.TestClass.TestRuntimeException;
import io.shiftleft.bctrace.filter.MethodFilter.DirectMethodFilter;
import io.shiftleft.bctrace.hook.DirectMethodHook;
import io.shiftleft.bctrace.hook.Hook;
import io.shiftleft.bctrace.runtime.listener.direct.$io_shiftleft_bctrace_direct_method_DirectMethodThrowableTest$DirectListenerModifiedThrowable;
import io.shiftleft.bctrace.runtime.listener.direct.$io_shiftleft_bctrace_direct_method_DirectMethodThrowableTest$DirectListenerThrowable;
import io.shiftleft.bctrace.runtime.listener.direct.$io_shiftleft_bctrace_direct_method_DirectMethodThrowableTest$DirectListenerThrowableRised;
import io.shiftleft.bctrace.runtime.listener.direct.DirectMethodThrowableListener;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import org.junit.Test;

/**
 * @author Ignacio del Valle Alles idelvall@shiftleft.io
 */
public class DirectMethodThrowableTest extends BcTraceTest {

  @Test
  public void testConstructorUncaughtThrowable() throws Exception {
    final StringBuilder sb = new StringBuilder();

    Class clazz = getInstrumentClass(TestClass.class, new Hook[]{
        new DirectMethodHook(
            new DirectMethodFilter("io/shiftleft/bctrace/TestClass", "<init>",
                "(I)V", false),
            new DirectListenerThrowable(sb))});

    boolean captured = false;
    try {
      Constructor constructor = clazz.getConstructor(int.class);
      constructor.newInstance(2);
    } catch (InvocationTargetException ite) {
      if (ite.getTargetException() instanceof TestRuntimeException) {
        captured = true;
        assertEquals(sb.toString(), ite.getTargetException().getMessage());
      }
    }
    assertTrue("Expected exception", captured);
  }

  @Test
  public void testConstructorModifiedThrowable() throws Exception {
    Class clazz = getInstrumentClass(TestClass.class, new Hook[]{
        new DirectMethodHook(
            new DirectMethodFilter("io/shiftleft/bctrace/TestClass", "<init>",
                "(I)V", false),
            new DirectListenerModifiedThrowable())});

    boolean captured = false;
    try {
      Constructor constructor = clazz.getConstructor(int.class);
      constructor.newInstance(2);
    } catch (InvocationTargetException ite) {
      if (ite.getTargetException() instanceof RuntimeException) {
        captured = true;
        assertEquals("Modified!", ite.getTargetException().getMessage());
      }
    }
    assertTrue("Expected exception", captured);
  }

  @Test
  public void testThrowableInListener() throws Exception {
    Class clazz = getInstrumentClass(TestClass.class, new Hook[]{
        new DirectMethodHook(
            new DirectMethodFilter("io/shiftleft/bctrace/TestClass", "<init>",
                "(I)V", false),
            new DirectListenerThrowableRised())});

    boolean captured = false;
    try {
      Constructor constructor = clazz.getConstructor(int.class);
      constructor.newInstance(2);
    } catch (InvocationTargetException ite) {
      if (ite.getTargetException() instanceof TestRuntimeException) {
        captured = true;
      }
    }
    assertTrue("Expected exception", captured);
  }

  public static class DirectListenerThrowable extends DirectMethodThrowableListener implements
      $io_shiftleft_bctrace_direct_method_DirectMethodThrowableTest$DirectListenerThrowable {

    private final StringBuilder sb;

    public DirectListenerThrowable(StringBuilder sb) {
      this.sb = sb;
    }

    @ListenerMethod
    public Throwable onFinish(Class clazz, Object instance, int i, Throwable th) {
      sb.append(th.getMessage());
      assertEquals(clazz.getName(), TestClass.class.getName());
      assertNull(instance);
      return th;
    }
  }

  public static class DirectListenerModifiedThrowable extends
      DirectMethodThrowableListener implements
      $io_shiftleft_bctrace_direct_method_DirectMethodThrowableTest$DirectListenerModifiedThrowable {

    @ListenerMethod
    public Throwable onFinish(Class clazz, Object instance, int i, Throwable th) {
      return new RuntimeException("Modified!");
    }
  }

  public static class DirectListenerThrowableRised extends DirectMethodThrowableListener implements
      $io_shiftleft_bctrace_direct_method_DirectMethodThrowableTest$DirectListenerThrowableRised {

    @ListenerMethod
    public Throwable onFinish(Class clazz, Object instance, int i, Throwable th) {
      throw new RuntimeException("Unexpected!");
    }
  }
}