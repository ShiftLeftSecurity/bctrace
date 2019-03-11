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
import io.shiftleft.bctrace.hierarchy.BctraceClass;
import io.shiftleft.bctrace.hook.DirectCallSiteHook;
import io.shiftleft.bctrace.hook.Hook;
import io.shiftleft.bctrace.runtime.listener.direct.$io_shiftleft_bctrace_direct_callsite_DirectCallSiteReturnTest$ArrayCopyListener;
import io.shiftleft.bctrace.runtime.listener.direct.$io_shiftleft_bctrace_direct_callsite_DirectCallSiteReturnTest$TestBarCallSiteMutableListener;
import io.shiftleft.bctrace.runtime.listener.direct.DirectCallSiteReturnListener;
import jdk.nashorn.internal.codegen.types.Type;
import org.junit.Test;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * @author Ignacio del Valle Alles idelvall@shiftleft.io
 */
public class DirectCallSiteReturnTest extends BcTraceTest {

  @Test
  public void testAfterVoidCallSite() throws Exception {
    StringBuilder sb = new StringBuilder();
    ArrayCopyListener l1 = new ArrayCopyListener("1", sb);
    ArrayCopyListener l2 = new ArrayCopyListener("2", sb);
    CallSiteFilter arrayCopyCallSiteFilter = new CallSiteFilter("java/lang/System", "arraycopy",
        "(Ljava/lang/Object;ILjava/lang/Object;II)V") {
      @Override
      public boolean acceptMethod(ClassNode cn, MethodNode mn) {
        return true;
      }
    };
    Class clazz = getInstrumentClass(TestClass.class, new Hook[]{
        new DirectCallSiteHook(arrayCopyCallSiteFilter, l1),
        new DirectCallSiteHook(arrayCopyCallSiteFilter, l2)
    }, false);

    clazz.getMethod("arrayCopyWrapper2").invoke(null);
    assertEquals("21", sb.toString());
  }


  public static class ArrayCopyListener extends DirectCallSiteReturnListener implements
      $io_shiftleft_bctrace_direct_callsite_DirectCallSiteReturnTest$ArrayCopyListener {

    private final String token;
    private final StringBuilder sb;

    public ArrayCopyListener(String token, StringBuilder sb) {
      this.token = token;
      this.sb = sb;
    }

    @ListenerMethod
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
    TestBarCallSiteMutableListener listener = new TestBarCallSiteMutableListener();
    Class clazz = getInstrumentClass(TestClass.class, new Hook[]{
        new DirectCallSiteHook(new CallSiteFilter(Type.getInternalName(TestClass.class), "bar",
            "(Ljava/lang/String;)I") {
          @Override
          public boolean acceptMethod(ClassNode cn, MethodNode mn) {
            return true;
          }
        },
            listener)
    }, false);

    int ret = (Integer) clazz.getMethod("foo", String.class).invoke(null, "123");
    assertEquals(15, ret);
  }

  public static class TestBarCallSiteMutableListener extends DirectCallSiteReturnListener implements
      $io_shiftleft_bctrace_direct_callsite_DirectCallSiteReturnTest$TestBarCallSiteMutableListener {


    @ListenerMethod
    public int onAfterCall(Class clazz, Object instance,
        Object callSiteInstance, String s, int ret) {
      return 5;
    }
  }
}
