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
package io.shiftleft.bctrace.generic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import io.shiftleft.bctrace.BcTraceTest;
import io.shiftleft.bctrace.TestClass;
import io.shiftleft.bctrace.TestClass.TestRuntimeException;
import io.shiftleft.bctrace.filter.AllFilter;
import io.shiftleft.bctrace.filter.Filter;
import io.shiftleft.bctrace.hierarchy.BctraceClass;
import io.shiftleft.bctrace.hook.Hook;
import io.shiftleft.bctrace.hook.GenericHook;
import io.shiftleft.bctrace.runtime.listener.generic.FinishListener;
import io.shiftleft.bctrace.runtime.listener.generic.GenericListener;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import org.junit.Test;
import org.objectweb.asm.tree.MethodNode;

/**
 * @author Ignacio del Valle Alles idelvall@shiftleft.io
 */
public class FinishListenerTest extends BcTraceTest {

  @Test
  public void testReturnDisabled() throws Exception {
    final StringBuilder steps = new StringBuilder();
    Class clazz = getInstrumentClass(TestClass.class, new Hook[]{
        new GenericHook() {
          @Override
          public Filter getFilter() {
            return new AllFilter();
          }

          @Override
          public GenericListener getListener() {
            return new FinishListener() {
              @Override
              public boolean requiresReturnedValue() {
                return false;
              }

              @Override
              public Object onFinish(int methodId, Class clazz,
                  Object instance, Object[] args,
                  Object ret, Throwable th) {
                if (args == null) {
                  steps.append("1");
                }
                return ret;
              }
            };
          }
        }
    });
    clazz.getMethod("execVoid").invoke(null);
    assertEquals("1", steps.toString());
  }


  @Test
  public void testReturnModification() throws Exception {
    final StringBuilder steps = new StringBuilder();
    Class clazz = getInstrumentClass(TestClass.class, new Hook[]{
        new GenericHook() {
          @Override
          public Filter getFilter() {
            return new AllFilter();
          }

          @Override
          public GenericListener getListener() {
            return new FinishListener() {
              @Override
              public Object onFinish(int methodId, Class clazz, Object instance, Object[] args,
                  Object ret, Throwable th) {
                Long value = (Long) ret;
                assertEquals((long) value, 3l);
                return value + 1;
              }
            };
          }
        },
        new GenericHook() {
          @Override
          public Filter getFilter() {
            return new AllFilter();
          }

          @Override
          public GenericListener getListener() {
            return new FinishListener() {
              @Override
              public Object onFinish(int methodId, Class clazz, Object instance, Object[] args,
                  Object ret, Throwable th) {
                Long value = (Long) ret;
                assertEquals((long) value, 2l);
                return value + 1;
              }
            };
          }
        }
    });
    Object ret = clazz.getMethod("getLong").invoke(null);
    Long value = (Long) ret;
    assertEquals((long) value, 4l);
  }

  @Test
  public void testReturn() throws Exception {
    final StringBuilder steps = new StringBuilder();
    Class clazz = getInstrumentClass(TestClass.class, new Hook[]{
        new GenericHook() {
          @Override
          public Filter getFilter() {
            return new AllFilter();
          }

          @Override
          public GenericListener getListener() {
            return new FinishListener() {
              @Override
              public Object onFinish(int methodId, Class clazz, Object instance, Object[] args,
                  Object ret, Throwable th) {
                assertEquals(clazz.getName(), TestClass.class.getName());
                steps.append("1");
                return ret;
              }
            };
          }
        },
        new GenericHook() {
          @Override
          public Filter getFilter() {
            return new AllFilter();
          }

          @Override
          public GenericListener getListener() {
            return new FinishListener() {
              @Override
              public Object onFinish(int methodId, Class clazz, Object instance, Object[] args,
                  Object ret, Throwable th) {
                assertEquals(clazz.getName(), TestClass.class.getName());
                steps.append("2");
                return ret;
              }
            };
          }
        }
    });
    clazz.getMethod("execVoid").invoke(null);
    System.out.println(clazz.getClassLoader());
    assertEquals("21", steps.toString());
  }

  public void testVoidReturn() throws Exception {
    final StringBuilder steps = new StringBuilder();
    Class clazz = getInstrumentClass(TestClass.class, new Hook[]{
        new GenericHook() {
          @Override
          public Filter getFilter() {
            return new AllFilter();
          }

          @Override
          public GenericListener getListener() {
            return new FinishListener() {
              @Override
              public Object onFinish(int methodId, Class clazz, Object instance, Object[] args,
                  Object ret, Throwable th) {
                assertNull(ret);
                assertNull(th);
                steps.append("1");
                return ret;
              }
            };
          }
        },
        new GenericHook() {
          @Override
          public Filter getFilter() {
            return new AllFilter();
          }

          @Override
          public GenericListener getListener() {
            return new FinishListener() {
              @Override
              public Object onFinish(int methodId, Class clazz, Object instance, Object[] args,
                  Object ret, Throwable th) {
                assertNull(ret);
                steps.append("2");
                return ret;
              }
            };
          }
        }
    });
    clazz.getMethod("execVoid").invoke(null);
    assertEquals("21", steps.toString());
  }


  @Test
  public void testSimpleUncaughtThrowable() throws Exception {
    final StringBuilder steps = new StringBuilder();
    Class clazz = getInstrumentClass(TestClass.class, new Hook[]{
        new GenericHook() {
          @Override
          public Filter getFilter() {
            return new AllFilter() {
              @Override
              public boolean instrumentMethod(BctraceClass clazz, MethodNode mn) {
                return mn.name.equals("getLongWithConditionalException");
              }
            };
          }

          @Override
          public GenericListener getListener() {
            return new FinishListener() {
              @Override
              public Object onFinish(int methodId, Class clazz, Object instance, Object[] args,
                  Object ret, Throwable th) {
                assertNotNull(th);
                steps.append("1t");
                return th;
              }
            };
          }
        }
    });
    boolean captured = false;
    try {
      clazz.getMethod("getLongWithConditionalException", boolean.class)
          .invoke(null, true);
    } catch (InvocationTargetException ite) {
      if (ite.getTargetException() instanceof TestRuntimeException) {
        captured = true;
        assertEquals("1t", steps.toString());
      }
    }
    assertTrue("Expected exception", captured);
  }

  @Test
  public void testConstructorUncaughtThrowable() throws Exception {
    final StringBuilder sb = new StringBuilder();

    Class clazz = getInstrumentClass(TestClass.class, new Hook[]{
        new GenericHook() {
          @Override
          public Filter getFilter() {
            return new AllFilter() {
              @Override
              public boolean instrumentMethod(BctraceClass clazz, MethodNode mn) {
                return mn.name.equals("<init>");
              }
            };
          }

          @Override
          public GenericListener getListener() {
            return new FinishListener() {
              @Override
              public Object onFinish(int methodId, Class clazz, Object instance, Object[] args,
                  Object ret, Throwable th) {
                assertNotNull(th);
                sb.append(th.getMessage());
                return th;
              }
            };
          }
        }
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
  public void testFinishReturnModification() throws Exception {
    final StringBuilder steps = new StringBuilder();
    Class clazz = getInstrumentClass(TestClass.class, new Hook[]{
        new GenericHook() {
          @Override
          public Filter getFilter() {
            return new AllFilter();
          }

          @Override
          public GenericListener getListener() {
            return new FinishListener() {
              @Override
              public Object onFinish(int methodId, Class clazz, Object instance, Object[] args,
                  Object ret, Throwable th) {
                Long value = (Long) ret;
                assertEquals((long) value, 3l);
                return value + 1;
              }
            };
          }
        },
        new GenericHook() {
          @Override
          public Filter getFilter() {
            return new AllFilter();
          }

          @Override
          public GenericListener getListener() {
            return new FinishListener() {
              @Override
              public Object onFinish(int methodId, Class clazz, Object instance, Object[] args,
                  Object ret, Throwable th) {
                Long value = (Long) ret;
                assertEquals((long) value, 2l);
                return value + 1;
              }
            };
          }
        }
    });
    Object ret = clazz.getMethod("getLong").invoke(null);
    Long value = (Long) ret;
    assertEquals((long) value, 4l);
  }

  @Test
  public void testListenerExceptionOnReturn() throws Exception {
    final StringBuilder sb = new StringBuilder();
    Class clazz = getInstrumentClass(TestClass.class, new Hook[]{
        new GenericHook() {
          @Override
          public Filter getFilter() {
            return new AllFilter();
          }

          @Override
          public GenericListener getListener() {
            return new FinishListener() {
              @Override
              public Object onFinish(int methodId, Class clazz, Object instance, Object[] args,
                  Object ret, Throwable th) {
                sb.append(ret);
                throw new RuntimeException("Unexpected!");
              }
            };
          }
        }
    });
    Long ret = (Long) clazz.getMethod("getLong").invoke(null);
    assertEquals(ret.toString(), sb.toString());
  }

  @Test
  public void testFinishException() throws Exception {
    final StringBuilder steps = new StringBuilder();
    Class clazz = getInstrumentClass(TestClass.class, new Hook[]{
        new GenericHook() {
          @Override
          public Filter getFilter() {
            return new AllFilter() {
              @Override
              public boolean instrumentMethod(BctraceClass clazz, MethodNode mn) {
                return mn.name.equals("<init>");
              }
            };
          }

          @Override
          public GenericListener getListener() {
            return new FinishListener() {
              @Override
              public Object onFinish(int methodId, Class clazz, Object instance, Object[] args,
                  Object ret, Throwable th) {
                steps.append("1");
                assertNotNull(th);
                return th;
              }
            };
          }
        }
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
        new GenericHook() {
          @Override
          public Filter getFilter() {
            return new AllFilter() {
              @Override
              public boolean instrumentMethod(BctraceClass clazz, MethodNode mn) {
                return mn.name.equals("<init>");
              }
            };
          }

          @Override
          public GenericListener getListener() {
            return new FinishListener() {
              @Override
              public Object onFinish(int methodId, Class clazz, Object instance, Object[] args,
                  Object ret, Throwable th) {
                steps.append("1");
                throw new RuntimeException("Unexpected!");
              }
            };
          }
        }
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
        new GenericHook() {
          @Override
          public Filter getFilter() {
            return new AllFilter() {
              @Override
              public boolean instrumentMethod(BctraceClass clazz, MethodNode mn) {
                return mn.name.equals("<init>");
              }
            };
          }

          @Override
          public GenericListener getListener() {
            return new FinishListener() {
              @Override
              public Object onFinish(int methodId, Class clazz, Object instance, Object[] args,
                  Object ret, Throwable th) {
                assertNotNull(th);
                sb.append(th.getMessage());
                return new IOException(th.getMessage(), th);
              }
            };
          }
        }
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
