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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import io.shiftleft.bctrace.BcTraceTest;
import io.shiftleft.bctrace.TestClass;
import io.shiftleft.bctrace.TestClass.TestRuntimeException;
import io.shiftleft.bctrace.filter.MethodFilter.AllFilter;
import io.shiftleft.bctrace.hierarchy.BctraceClass;
import io.shiftleft.bctrace.hook.GenericMethodHook;
import io.shiftleft.bctrace.hook.Hook;
import io.shiftleft.bctrace.runtime.listener.generic.GenericMethodThrowableListener;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import org.junit.Test;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * @author Ignacio del Valle Alles idelvall@shiftleft.io
 */
public class GenericMethodThrowableTest extends BcTraceTest {

  @Test
  public void testSimpleUncaughtThrowable() throws Exception {
    final StringBuilder steps = new StringBuilder();
    Class clazz = getInstrumentClass(TestClass.class, new Hook[]{
        new GenericMethodHook(
            new AllFilter() {
              @Override
              public boolean acceptMethod(ClassNode cn, MethodNode mn) {
                return mn.name.equals("getLongWithConditionalException");
              }
            },
            new GenericMethodThrowableListener() {
              @Override
              public Throwable onThrow(int methodId, Class clazz, Object instance, Object[] args,
                  Throwable th) {
                assertNotNull(th);
                steps.append("1t");
                return th;
              }
            }
        )
    });
    boolean captured = false;
    try {
      clazz.getMethod("getLongWithConditionalException", boolean.class)
          .invoke(null, true);
    } catch (InvocationTargetException ite) {
      if (ite.getTargetException() instanceof TestRuntimeException) {
        captured = true;
        assertEquals("1t", steps.toString());
      } else {
        ite.printStackTrace();
      }
    }
    assertTrue("Expected exception", captured);
  }

  @Test
  public void testConstructorUncaughtThrowable() throws Exception {
    final StringBuilder sb = new StringBuilder();

    Class clazz = getInstrumentClass(TestClass.class, new Hook[]{
        new GenericMethodHook(
            new AllFilter() {
              @Override
              public boolean acceptMethod(ClassNode cn, MethodNode mn) {
                return mn.name.equals("<init>");
              }
            },
            new GenericMethodThrowableListener() {
              @Override
              public Throwable onThrow(int methodId, Class clazz, Object instance, Object[] args,
                  Throwable th) {
                assertNotNull(th);
                sb.append(th.getMessage());
                return th;
              }
            }
        )
    });
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
  public void testFinishException() throws Exception {
    final StringBuilder steps = new StringBuilder();
    Class clazz = getInstrumentClass(TestClass.class, new Hook[]{
        new GenericMethodHook(
            new AllFilter() {
              @Override
              public boolean acceptMethod(ClassNode cn, MethodNode mn) {
                return mn.name.equals("<init>");
              }
            },
            new GenericMethodThrowableListener() {
              @Override
              public Throwable onThrow(int methodId, Class clazz, Object instance, Object[] args,
                  Throwable th) {
                steps.append("1");
                assertNotNull(th);
                return th;
              }
            }
        )
    });
    boolean captured = false;
    try {
      Constructor constructor = clazz.getConstructor(int.class);
      constructor.newInstance(2);
    } catch (InvocationTargetException ite) {
      if (ite.getTargetException() instanceof TestRuntimeException) {
        captured = true;
        assertEquals(steps.toString(), "1");
      }
    }
    assertTrue("Expected exception", captured);
  }

  @Test
  public void testListenerExceptionOnThrowable() throws Exception {
    final StringBuilder steps = new StringBuilder();
    Class clazz = getInstrumentClass(TestClass.class, new Hook[]{
        new GenericMethodHook(
            new AllFilter() {
              @Override
              public boolean acceptMethod(ClassNode cn, MethodNode mn) {
                return mn.name.equals("<init>");
              }
            },
            new GenericMethodThrowableListener() {
              @Override
              public Throwable onThrow(int methodId, Class clazz, Object instance, Object[] args,
                  Throwable th) {
                steps.append("1");
                throw new RuntimeException("Unexpected!");
              }
            }
        )
    });
    boolean captured = false;
    try {
      Constructor constructor = clazz.getConstructor(int.class);
      constructor.newInstance(2);
    } catch (InvocationTargetException ite) {
      if (ite.getTargetException() instanceof TestRuntimeException) {
        captured = true;
        assertEquals(steps.toString(), "1");
      }
    }
    assertTrue("Expected exception", captured);
  }

  @Test
  public void testFinishExceptionModification() throws Exception {
    final StringBuilder sb = new StringBuilder();
    Class clazz = getInstrumentClass(TestClass.class, new Hook[]{
        new GenericMethodHook(
            new AllFilter() {
              @Override
              public boolean acceptMethod(ClassNode cn, MethodNode mn) {
                return mn.name.equals("<init>");
              }
            },
            new GenericMethodThrowableListener() {
              @Override
              public Throwable onThrow(int methodId, Class clazz, Object instance, Object[] args,
                  Throwable th) {
                assertNotNull(th);
                sb.append(th.getMessage());
                return new IOException(th.getMessage(), th);
              }
            }
        )
    });
    boolean captured = false;
    try {
      Constructor constructor = clazz.getConstructor(int.class);
      constructor.newInstance(2);
    } catch (InvocationTargetException ite) {
      if (ite.getTargetException() instanceof IOException) {
        captured = true;
        assertEquals(sb.toString(), ite.getTargetException().getMessage());
      }
    }
    assertTrue("Expected exception", captured);
  }

}
