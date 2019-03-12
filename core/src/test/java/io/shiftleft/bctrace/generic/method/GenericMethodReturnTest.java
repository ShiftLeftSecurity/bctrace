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
package io.shiftleft.bctrace.generic.method;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import io.shiftleft.bctrace.BcTraceTest;
import io.shiftleft.bctrace.TestClass;
import io.shiftleft.bctrace.filter.MethodFilter.AllFilter;
import io.shiftleft.bctrace.hook.GenericMethodHook;
import io.shiftleft.bctrace.hook.Hook;
import io.shiftleft.bctrace.runtime.BctraceRuntimeException;
import io.shiftleft.bctrace.runtime.listener.generic.GenericMethodReturnListener;
import java.lang.reflect.InvocationTargetException;
import org.junit.Test;

/**
 * @author Ignacio del Valle Alles idelvall@shiftleft.io
 */
public class GenericMethodReturnTest extends BcTraceTest {

  @Test
  public void testNoArguments() throws Exception {
    final StringBuilder steps = new StringBuilder();
    Class clazz = getInstrumentClass(TestClass.class, new Hook[]{
        new GenericMethodHook(
            new AllFilter(),
            new GenericMethodReturnListener() {
              @Override
              public boolean requiresReturnedValue() {
                return false;
              }

              @Override
              public Object onReturn(int methodId, Class clazz, Object instance, Object[] args,
                  Object ret) {
                if (args == null) {
                  steps.append("1");
                }
                return ret;
              }
            }
        )
    });
    clazz.getMethod("execVoid").invoke(null);
    assertEquals("1", steps.toString());
  }

  @Test
  public void testVoidReturn() throws Exception {
    final StringBuilder steps = new StringBuilder();
    Class clazz = getInstrumentClass(TestClass.class, new Hook[]{
        new GenericMethodHook(
            new AllFilter(),
            new GenericMethodReturnListener() {
              @Override
              public Object onReturn(int methodId, Class clazz, Object instance, Object[] args,
                  Object ret) {
                assertNull(ret);
                steps.append("1");
                return ret;
              }
            }
        ),
        new GenericMethodHook(
            new AllFilter(),
            new GenericMethodReturnListener() {
              @Override
              public Object onReturn(int methodId, Class clazz, Object instance, Object[] args,
                  Object ret) {
                assertNull(ret);
                steps.append("2");
                return ret;
              }
            }
        )
    });
    clazz.getMethod("execVoid").invoke(null);
    assertEquals("21", steps.toString());
  }

  @Test
  public void testReturnValueModification() throws Exception {
    final StringBuilder steps = new StringBuilder();
    Class clazz = getInstrumentClass(TestClass.class, new Hook[]{
            new GenericMethodHook(
                new AllFilter(),
                new GenericMethodReturnListener() {
                  @Override
                  public Object onReturn(int methodId, Class clazz, Object instance, Object[] args,
                      Object ret) {
                    Long value = (Long) ret;
                    assertEquals((long) value, 3l);
                    return value + 1;
                  }
                }),
            new GenericMethodHook(
                new AllFilter(),
                new GenericMethodReturnListener() {
                  @Override
                  public Object onReturn(int methodId, Class clazz, Object instance, Object[] args,
                      Object ret) {
                    Long value = (Long) ret;
                    assertEquals((long) value, 2l);
                    return value + 1;
                  }
                })
        }
    );
    Object ret = clazz.getMethod("getLong").invoke(null);
    Long value = (Long) ret;
    assertEquals((long) value, 4l);
  }

  @Test
  public void testListeneUnexpectedException() throws Exception {
    final StringBuilder sb = new StringBuilder();
    Class clazz = getInstrumentClass(TestClass.class, new Hook[]{
        new GenericMethodHook(
            new AllFilter(),
            new GenericMethodReturnListener() {
              @Override
              public Object onReturn(int methodId, Class clazz, Object instance, Object[] args,
                  Object ret) {
                sb.append(ret);
                throw new RuntimeException("Unexpected!");
              }
            }
        )
    });
    Long ret = (Long) clazz.getMethod("getLong").invoke(null);
    assertEquals(ret.toString(), sb.toString());
  }

  @Test
  public void testListeneExpectedException() throws Exception {
    final StringBuilder steps = new StringBuilder();
    final RuntimeException re = new RuntimeException("Expected!");
    Class clazz = getInstrumentClass(TestClass.class, new Hook[]{
        new GenericMethodHook(
            new AllFilter(),
            new GenericMethodReturnListener() {
              @Override
              public Object onReturn(int methodId, Class clazz, Object instance, Object[] args,
                  Object ret) {
                steps.append("1");
                throw new BctraceRuntimeException(re);
              }
            }
        )
    });
    try {
      Long ret = (Long) clazz.getMethod("getLong").invoke(null);
    } catch (InvocationTargetException ite) {
      steps.append("2");
      assertTrue(ite.getTargetException() == re);
    }

    assertEquals(steps.toString(), "12");
  }
}
