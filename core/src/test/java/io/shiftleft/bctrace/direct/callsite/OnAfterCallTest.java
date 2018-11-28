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
import io.shiftleft.bctrace.filter.AllFilter;
import io.shiftleft.bctrace.hook.Hook;
import io.shiftleft.bctrace.hook.direct.CallSiteHook;
import io.shiftleft.bctrace.runtime.listener.direct.CallSiteListener;
import jdk.nashorn.internal.codegen.types.Type;
import org.junit.Test;

/**
 * @author Ignacio del Valle Alles idelvall@shiftleft.io
 */
public class OnAfterCallTest extends BcTraceTest {

  @Test
  public void testAfterVoidCallSite() throws Exception {
    StringBuilder sb = new StringBuilder();
    AfterCallSiteListener l1 = new AfterCallSiteListener("1", sb);
    AfterCallSiteListener l2 = new AfterCallSiteListener("2", sb);
    Class clazz = getInstrumentClass(TestClass.class, new Hook[]{
        new CallSiteHook(new AllFilter(), l1),
        new CallSiteHook(new AllFilter(), l2)
    }, false);

    clazz.getMethod("arrayCopyWrapper2").invoke(null);
    assertEquals("21", sb.toString());
  }

  /**
   * This accessory interface is needed for testing purposes only. The agent will generate it on
   * CallbackTransformer.class at runtime
   */
  public static interface AfterCallSiteListenerInterface {

    public void onAfterCall(Class clazz, Object instance,
        Object callSiteInstance, Object src, int srcPos,
        Object dest, int destPos,
        int length);
  }

  public static class AfterCallSiteListener extends CallSiteListener implements
      AfterCallSiteListenerInterface {

    private final String token;
    private final StringBuilder sb;

    public AfterCallSiteListener(String token, StringBuilder sb) {
      this.token = token;
      this.sb = sb;
    }

    @Override
    public String getCallSiteClassName() {
      return "java/lang/System";
    }

    @Override
    public String getCallSiteMethodName() {
      return "arraycopy";
    }

    @Override
    public String getCallSiteMethodDescriptor() {
      return "(Ljava/lang/Object;ILjava/lang/Object;II)V";
    }

    @ListenerMethod(type = ListenerType.onAfterCall)
    public void onAfterCall(Class clazz, Object instance,
        Object callSiteInstance, Object src, int srcPos,
        Object dest, int destPos,
        int length) {
      sb.append(token);
      assertEquals(clazz.getName(), TestClass.class.getName());
      assertEquals(instance.getClass().getName(), TestClass.class.getName());
      assertTrue(callSiteInstance == null);
      assertNotNull(src);
      assertNotNull(dest);
    }
  }


  @Test
  public void testReturnVoidCallSite() throws Exception {
    AfterCallSiteMutableListener listener = new AfterCallSiteMutableListener();
    Class clazz = getInstrumentClass(TestClass.class, new Hook[]{
        new CallSiteHook(new AllFilter(), listener)
    }, false);

    int ret = (Integer) clazz.getMethod("foo", String.class).invoke(null, "123");
    assertEquals(15, ret);
  }

  /**
   * This accessory interface is needed for testing purposes only. The agent will generate it on
   * CallbackTransformer.class at runtime
   */
  public static interface AfterCallSiteMutableListenerInterface {

    public int onAfterCall(Class clazz, Object instance,
        Object callSiteInstance, String s, int ret);
  }

  public static class AfterCallSiteMutableListener extends CallSiteListener implements
      AfterCallSiteMutableListenerInterface {

    @Override
    public String getCallSiteClassName() {
      return Type.getInternalName(TestClass.class);
    }

    @Override
    public String getCallSiteMethodName() {
      return "bar";
    }

    @Override
    public String getCallSiteMethodDescriptor() {
      return "(Ljava/lang/String;)I";
    }

    @ListenerMethod(type = ListenerType.onAfterCall)
    public int onAfterCall(Class clazz, Object instance,
        Object callSiteInstance, String s, int ret) {
      return 5;
    }
  }
}
