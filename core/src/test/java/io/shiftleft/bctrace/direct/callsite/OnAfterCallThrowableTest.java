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
import io.shiftleft.bctrace.TestClass.TestRuntimeException;
import io.shiftleft.bctrace.direct.method.OnThrowableTest.DirectListenerThrowableRised;
import io.shiftleft.bctrace.filter.AllFilter;
import io.shiftleft.bctrace.filter.MethodFilter;
import io.shiftleft.bctrace.hook.Hook;
import io.shiftleft.bctrace.hook.direct.CallSiteHook;
import io.shiftleft.bctrace.hook.direct.MethodHook;
import io.shiftleft.bctrace.runtime.listener.direct.CallSiteListener;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import org.junit.Test;

/**
 * @author Ignacio del Valle Alles idelvall@shiftleft.io
 */
public class OnAfterCallThrowableTest extends BcTraceTest {

  @Test
  public void testCallSite() throws Exception {
    StringBuilder sb = new StringBuilder();
    CallSiteListener callSiteListener1 = new CallSiteListener1(sb);
    Class clazz = getInstrumentClass(TestClass.class, new Hook[]{
        new CallSiteHook(new AllFilter(), callSiteListener1)
    }, false);

    try {
      clazz.getMethod("getLongWithConditionalException", boolean.class).invoke(null, true);
    } catch (InvocationTargetException iex) {
      assertEquals(TestClass.RTE_MESSAGE, iex.getTargetException().getMessage());
    }
    assertEquals(TestClass.RTE_MESSAGE, sb.toString());
  }

  /**
   * This accessory interface is needed for testing purposes only. The agent will generate it on
   * CallbackTransformer.class at runtime
   */
  public static interface CallSiteListener1Interface {

    public Throwable onAfterCall(Class clazz, Object instance,
        Object callSiteInstance, Throwable th);
  }

  public static class CallSiteListener1 extends CallSiteListener implements
      CallSiteListener1Interface {

    private final StringBuilder sb;

    public CallSiteListener1(StringBuilder sb) {
      this.sb = sb;
    }

    @Override
    public String getCallSiteClassName() {
      return "io/shiftleft/bctrace/TestClass";
    }

    @Override
    public String getCallSiteMethodName() {
      return "throwRuntimeException";
    }

    @Override
    public String getCallSiteMethodDescriptor() {
      return "()V";
    }

    @ListenerMethod(type = ListenerType.onAfterCallThrowable)
    public Throwable onAfterCall(Class clazz, Object instance,
        Object callSiteInstance, Throwable th) {
      sb.append(th.getMessage());
      return th;
    }
  }
}