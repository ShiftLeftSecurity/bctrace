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
import io.shiftleft.bctrace.filter.MethodFilter;
import io.shiftleft.bctrace.hook.Hook;
import io.shiftleft.bctrace.hook.direct.MethodHook;
import io.shiftleft.bctrace.runtime.listener.direct.DirectListener;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import org.junit.Test;

/**
 * @author Ignacio del Valle Alles idelvall@shiftleft.io
 */
public class OnThrowableTest extends BcTraceTest {

  @Test
  public void testConstructorUncaughtThrowable() throws Exception {
    final StringBuilder sb = new StringBuilder();

    Class clazz = getInstrumentClass(TestClass.class, new Hook[]{
        new MethodHook(
            new MethodFilter("io/shiftleft/bctrace/TestClass", "<init>",
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
        new MethodHook(
            new MethodFilter("io/shiftleft/bctrace/TestClass", "<init>",
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
        new MethodHook(
            new MethodFilter("io/shiftleft/bctrace/TestClass", "<init>",
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

  /**
   * This accessory interface is needed for testing purposes only. The agent will generate it on
   * CallbackTransformer.class at runtime
   */
  public static interface DirectListenerThrowableInterface {

    public Throwable onFinish(Class clazz, Object instance, Throwable th, int i);
  }


  public static class DirectListenerThrowable extends DirectListener implements
      DirectListenerThrowableInterface {

    private final StringBuilder sb;

    public DirectListenerThrowable(StringBuilder sb) {
      this.sb = sb;
    }

    @ListenerMethod(type = ListenerType.onThrowable)
    public Throwable onFinish(Class clazz, Object instance, Throwable th, int i) {
      sb.append(th.getMessage());
      assertEquals(clazz.getName(), TestClass.class.getName());
      assertNull(instance);
      return th;
    }
  }

  /**
   * This accessory interface is needed for testing purposes only. The agent will generate it on
   * CallbackTransformer.class at runtime
   */
  public static interface DirectListenerModifiedThrowableInterface {

    public Throwable onFinish(Class clazz, Object instance, Throwable th, int i);
  }

  public static class DirectListenerModifiedThrowable extends DirectListener implements
      DirectListenerModifiedThrowableInterface {

    @ListenerMethod(type = ListenerType.onThrowable)
    public Throwable onFinish(Class clazz, Object instance, Throwable th, int i) {
      return new RuntimeException("Modified!");
    }
  }

  /**
   * This accessory interface is needed for testing purposes only. The agent will generate it on
   * CallbackTransformer.class at runtime
   */
  public static interface DirectListenerThrowableRisedInterface {

    public Throwable onFinish(Class clazz, Object instance, Throwable th, int i);
  }

  public static class DirectListenerThrowableRised extends DirectListener implements
      DirectListenerThrowableRisedInterface {

    @ListenerMethod(type = ListenerType.onThrowable)
    public Throwable onFinish(Class clazz, Object instance, Throwable th, int i) {
      throw  new RuntimeException("Unexpected!");
    }
  }
}