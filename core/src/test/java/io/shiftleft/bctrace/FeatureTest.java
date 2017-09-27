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
 * of this source code, which includeas information that is confidential and/or proprietary, and
 * is a trade secret, of ShiftLeft, Inc.
 *
 * ANY REPRODUCTION, MODIFICATION, DISTRIBUTION, PUBLIC PERFORMANCE, OR PUBLIC DISPLAY
 * OF OR THROUGH USE OF THIS SOURCE CODE WITHOUT THE EXPRESS WRITTEN CONSENT OF ShiftLeft, Inc.
 * IS STRICTLY PROHIBITED, AND IN VIOLATION OF APPLICABLE LAWS AND INTERNATIONAL TREATIES.
 * THE RECEIPT OR POSSESSION OF THIS SOURCE CODE AND/OR RELATED INFORMATION DOES NOT
 * CONVEY OR IMPLY ANY RIGHTS TO REPRODUCE, DISCLOSE OR DISTRIBUTE ITS
 * CONTENTS, OR TO MANUFACTURE, USE, OR SELL ANYTHING THAT IT MAY DESCRIBE, IN WHOLE OR IN PART.
 */
package io.shiftleft.bctrace;

import java.lang.reflect.InvocationTargetException;
import io.shiftleft.bctrace.TestClass.TestRuntimeException;
import io.shiftleft.bctrace.spi.Filter;
import io.shiftleft.bctrace.spi.Hook;
import io.shiftleft.bctrace.impl.AllFilter;
import org.junit.Test;
import static org.junit.Assert.*;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import io.shiftleft.bctrace.runtime.listener.Listener;
import io.shiftleft.bctrace.runtime.listener.info.BeforeThrownListener;
import io.shiftleft.bctrace.runtime.listener.info.FinishReturnListener;
import io.shiftleft.bctrace.runtime.listener.info.FinishThrowableListener;
import io.shiftleft.bctrace.runtime.listener.info.StartListener;

/**
 *
 * @author Ignacio del Valle Alles idelvall@shiftleft.io
 */
public class FeatureTest extends BcTraceTest {

  @Test
  public void testStart() throws Exception {
    final StringBuilder steps = new StringBuilder();
    Class clazz = getInstrumentClass(TestClass.class, new Hook[]{
      new Hook() {
        @Override
        public Filter getFilter() {
          return new AllFilter();
        }

        @Override
        public Listener getListener() {
          return new StartListener() {
            @Override
            public void onStart(int methodId, Object instance) {
              assertTrue(methodId > 0);
              steps.append("1");
            }
          };
        }
      },
      new Hook() {
        @Override
        public Filter getFilter() {
          return new AllFilter();
        }

        @Override
        public Listener getListener() {
          return new StartListener() {
            @Override
            public void onStart(int methodId, Object instance) {
              assertTrue(methodId > 0);
              steps.append("2");
            }
          };
        }
      }
    });
    clazz.getMethod("execVoid").invoke(null);
    System.out.println(clazz.getClassLoader());
    assertEquals("12", steps.toString());
  }

  @Test
  public void testConstructor() throws Exception {
    Class clazz = getInstrumentClass(TestClass.class, new Hook[]{
      new Hook() {
        @Override
        public Filter getFilter() {
          return new AllFilter();
        }

        @Override
        public Listener getListener() {
          return null;
        }
      }
    });
    clazz.newInstance();
  }

  //@Test
  public void testConstructor2() throws Exception {
    final StringBuilder steps = new StringBuilder();
    Class clazz = getInstrumentClass(TestClass.class, new Hook[]{
      new Hook() {
        @Override
        public Filter getFilter() {
          return new AllFilter();
        }

        @Override
        public Listener getListener() {
          return new FinishThrowableListener() {
            @Override
            public void onFinishedThrowable(int methodId, Object instance, Throwable th) {
              assertTrue(th instanceof TestRuntimeException);
              assertTrue(methodId > 0);
              steps.append("2");
            }
          };
        }
      },
      new Hook() {
        @Override
        public Filter getFilter() {
          return new AllFilter();
        }

        @Override
        public Listener getListener() {
          return new BeforeThrownListener() {
            @Override
            public void onBeforeThrown(int methodId, Object instance, Throwable th) {
              assertTrue(th instanceof TestRuntimeException);
              assertTrue(methodId > 0);
              steps.append("1");
            }
          };
        }
      }
    });
    boolean captured = false;
    try {
      clazz.getConstructor(int.class).newInstance(3);
    } catch (InvocationTargetException ite) {
      if (ite.getTargetException() instanceof TestRuntimeException) {
        captured = true;
        assertEquals("12", steps.toString());
      }
    }
    assertTrue("Expected exception", captured);
  }

  @Test
  public void testThrown() throws Exception {
    final StringBuilder steps = new StringBuilder();
    Class clazz = getInstrumentClass(TestClass.class, new Hook[]{
      new Hook() {
        @Override
        public Filter getFilter() {
          return new AllFilter();
        }

        @Override
        public Listener getListener() {
          return new BeforeThrownListener() {
            @Override
            public void onBeforeThrown(int methodId, Object instance, Throwable th) {
              assertTrue(th instanceof TestRuntimeException);
              assertTrue(methodId > 0);
              steps.append("1");
            }
          };
        }
      },
      new Hook() {
        @Override
        public Filter getFilter() {
          return new AllFilter();
        }

        @Override
        public Listener getListener() {
          return new BeforeThrownListener() {
            @Override
            public void onBeforeThrown(int methodId, Object instance, Throwable th) {
              assertTrue(th instanceof TestRuntimeException);
              assertTrue(methodId > 0);
              steps.append("2");
            }
          };
        }
      }
    });
    boolean captured = false;
    try {
      clazz.getMethod("throwRuntimeException").invoke(null);
    } catch (InvocationTargetException ite) {
      if (ite.getTargetException() instanceof TestRuntimeException) {
        captured = true;
        assertEquals("12", steps.toString());
      }
    }
    assertTrue("Expected exception", captured);
  }

  @Test
  public void testVoidReturn() throws Exception {
    final StringBuilder steps = new StringBuilder();
    Class clazz = getInstrumentClass(TestClass.class, new Hook[]{
      new Hook() {
        @Override
        public Filter getFilter() {
          return new AllFilter();
        }

        @Override
        public Listener getListener() {
          return new FinishReturnListener() {
            @Override
            public void onFinishedReturn(int methodId, Object instance, Object ret) {
              assertNull(ret);
              assertTrue(methodId > 0);
              steps.append("1");
            }
          };
        }
      },
      new Hook() {
        @Override
        public Filter getFilter() {
          return new AllFilter();
        }

        @Override
        public Listener getListener() {
          return new FinishThrowableListener() {
            @Override
            public void onFinishedThrowable(int methodId, Object instance, Throwable th) {
              assertTrue("This should not called", false);
            }
          };
        }
      },
      new Hook() {
        @Override
        public Filter getFilter() {
          return new AllFilter();
        }

        @Override
        public Listener getListener() {
          return new FinishReturnListener() {
            @Override
            public void onFinishedReturn(int methodId, Object instance, Object ret) {
              assertNull(ret);
              assertTrue(methodId > 0);
              steps.append("2");
            }
          };
        }
      }
    });
    clazz.getMethod("execVoid").invoke(null);
    assertEquals("21", steps.toString());
  }

  @Test
  public void testReturn() throws Exception {
    final StringBuilder steps = new StringBuilder();
    Class clazz = getInstrumentClass(TestClass.class, new Hook[]{
      new Hook() {
        @Override
        public Filter getFilter() {
          return new AllFilter();
        }

        @Override
        public Listener getListener() {
          return new FinishReturnListener() {
            @Override
            public void onFinishedReturn(int methodId, Object instance, Object ret) {
              assertNotNull(ret);
              assertTrue(methodId > 0);
              steps.append("1");
            }
          };
        }
      },
      new Hook() {
        @Override
        public Filter getFilter() {
          return new AllFilter();
        }

        @Override
        public Listener getListener() {
          return new FinishReturnListener() {
            @Override
            public void onFinishedReturn(int methodId, Object instance, Object ret) {
              assertNotNull(ret);
              assertTrue(methodId > 0);
              steps.append("2");
            }
          };
        }
      }
    });
    clazz.getMethod("getLong").invoke(null);
    clazz.getMethod("getInt").invoke(null);
    clazz.getMethod("getObject").invoke(null);
    assertEquals(steps.toString(), "212121");
  }

  @Test
  public void testUncaughtThrowable() throws Exception {
    final StringBuilder steps = new StringBuilder();
    Class clazz = getInstrumentClass(TestClass.class, new Hook[]{
      new Hook() {
        @Override
        public Filter getFilter() {
          return new AllFilter();
        }

        @Override
        public Listener getListener() {
          return new FinishReturnListener() {
            @Override
            public void onFinishedReturn(int methodId, Object instance, Object ret) {
              assertNotNull(ret);
              assertTrue(methodId > 0);
              steps.append("1r");
            }
          };
        }
      },
      new Hook() {
        @Override
        public Filter getFilter() {
          return new AllFilter();
        }

        @Override
        public Listener getListener() {
          return new FinishThrowableListener() {
            @Override
            public void onFinishedThrowable(int methodId, Object instance, Throwable th) {
              assertNotNull(th);
              assertTrue(methodId > 0);
              steps.append("1t");
            }
          };
        }
      },
      new Hook() {
        @Override
        public Filter getFilter() {
          return new AllFilter() {
            @Override
            public boolean instrumentMethod(ClassNode classNode, MethodNode mn) {
              return mn.name.equals("getLongWithConditionalException");
            }
          };
        }

        @Override
        public Listener getListener() {
          return new FinishThrowableListener() {
            @Override
            public void onFinishedThrowable(int methodId, Object instance, Throwable th) {
              assertNotNull(th);
              assertTrue(methodId > 0);
              steps.append("2t");
            }
          };
        }
      },
      new Hook() {
        @Override
        public Filter getFilter() {
          return new AllFilter() {
            @Override
            public boolean instrumentMethod(ClassNode classNode, MethodNode mn) {
              return mn.name.equals("getLongWithConditionalException");
            }
          };
        }

        @Override
        public Listener getListener() {
          return new FinishReturnListener() {
            @Override
            public void onFinishedReturn(int methodId, Object instance, Object ret) {
              assertNotNull(ret);
              assertTrue(methodId > 0);
              steps.append("2r");
            }
          };
        }
      }
    });
    clazz.getMethod("getLongWithConditionalException", boolean.class).invoke(null, false);
    assertEquals("2r1r", steps.toString());
    steps.setLength(0);
    boolean captured = false;
    try {
      clazz.getMethod("getLongWithConditionalException", boolean.class).invoke(null, true);
      assertEquals(steps.toString(), "");
    } catch (InvocationTargetException ite) {
      if (ite.getTargetException() instanceof TestRuntimeException) {
        captured = true;
        assertEquals("1t2t1t", steps.toString());
      }
    }
    assertTrue("Expected exception", captured);
  }

}
