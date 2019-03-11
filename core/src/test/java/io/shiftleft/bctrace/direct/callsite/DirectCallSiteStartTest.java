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
import io.shiftleft.bctrace.hook.DirectCallSiteHook;
import io.shiftleft.bctrace.hook.Hook;
import io.shiftleft.bctrace.runtime.listener.direct.$io_shiftleft_bctrace_direct_callsite_DirectCallSiteStartTest$ArrayCopyListener;
import io.shiftleft.bctrace.runtime.listener.direct.$io_shiftleft_bctrace_direct_callsite_DirectCallSiteStartTest$PrintListener;
import io.shiftleft.bctrace.runtime.listener.direct.DirectCallSiteStartListener;
import org.junit.Test;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * @author Ignacio del Valle Alles idelvall@shiftleft.io
 */
public class DirectCallSiteStartTest extends BcTraceTest {

  @Test
  public void testCallSite() throws Exception {
    StringBuilder sb = new StringBuilder();
    ArrayCopyListener arrayCopyListener1 = new ArrayCopyListener(sb, "1");
    ArrayCopyListener arrayCopyListener2 = new ArrayCopyListener(sb, "2");
    CallSiteFilter arrayCopyFilter = new CallSiteFilter("java/lang/System", "arraycopy",
        "(Ljava/lang/Object;ILjava/lang/Object;II)V") {
      @Override
      public boolean acceptMethod(ClassNode cn, MethodNode mn) {
        return true;
      }
    };
    Class clazz = getInstrumentClass(TestClass.class, new Hook[]{
        new DirectCallSiteHook(arrayCopyFilter, arrayCopyListener1),
        new DirectCallSiteHook(arrayCopyFilter, arrayCopyListener2)
    }, false);

    clazz.getMethod("arrayCopyWrapper2").invoke(null);
    assertEquals("12", sb.toString());
  }

  public static class ArrayCopyListener extends DirectCallSiteStartListener implements
      $io_shiftleft_bctrace_direct_callsite_DirectCallSiteStartTest$ArrayCopyListener {

    private final StringBuilder sb;
    private final String message;

    public ArrayCopyListener(StringBuilder sb, String message) {
      this.sb = sb;
      this.message = message;
    }

    @ListenerMethod
    public void onBeforeCall(Class clazz, Object instance,
        Object callSiteInstance, Object src, int srcPos,
        Object dest, int destPos,
        int length) {
      sb.append(message);
      assertEquals(clazz.getName(), TestClass.class.getName());
      assertEquals(instance.getClass().getName(), TestClass.class.getName());
      assertTrue(callSiteInstance == null);
      assertNotNull(src);
      assertNotNull(dest);
    }
  }

  /**
   * Instruments only call sites in lines 152 and 154 (skipping line 153)
   */
  @Test
  public void testLineNumber() throws Exception {
    StringBuilder sb = new StringBuilder();
    PrintListener printListener = new PrintListener(sb);
    CallSiteFilter arrayCopyFilter = new CallSiteFilter(
        "io/shiftleft/bctrace/TestClass",
        "printMessage",
        "(Ljava/lang/String;)V",
        new int[]{152, 154}) {
      @Override
      public boolean acceptMethod(ClassNode cn, MethodNode mn) {
        return true;
      }
    };
    Class clazz = getInstrumentClass(TestClass.class, new Hook[]{
        new DirectCallSiteHook(arrayCopyFilter, printListener)
    }, false);

    clazz.getMethod("greet").invoke(null);
    assertEquals("Hello!", sb.toString());
  }

  public static class PrintListener extends DirectCallSiteStartListener implements
      $io_shiftleft_bctrace_direct_callsite_DirectCallSiteStartTest$PrintListener {

    private final StringBuilder sb;

    public PrintListener(StringBuilder sb) {
      this.sb = sb;
    }

    @ListenerMethod
    public void onStart(Class clazz, Object instance, Object callSiteInstance, String message) {
      sb.append(message);
      assertEquals(clazz.getName(), TestClass.class.getName());
      assertTrue(instance == null);
      assertTrue(callSiteInstance == null);
    }
  }
}
