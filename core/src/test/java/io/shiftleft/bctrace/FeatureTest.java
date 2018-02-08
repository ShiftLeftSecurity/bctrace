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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import io.shiftleft.bctrace.TestClass.TestRuntimeException;
import io.shiftleft.bctrace.impl.AllFilter;
import io.shiftleft.bctrace.runtime.listener.Listener;
import io.shiftleft.bctrace.runtime.listener.info.BeforeThrownListener;
import io.shiftleft.bctrace.runtime.listener.info.FinishReturnArgumentsListener;
import io.shiftleft.bctrace.runtime.listener.info.FinishReturnListener;
import io.shiftleft.bctrace.runtime.listener.info.FinishThrowableListener;
import io.shiftleft.bctrace.runtime.listener.info.StartListener;
import io.shiftleft.bctrace.runtime.listener.mut.StartMutableListener;
import io.shiftleft.bctrace.spi.Filter;
import io.shiftleft.bctrace.spi.Hook;
import io.shiftleft.bctrace.spi.hierarchy.UnloadedClassInfo;
import java.lang.reflect.InvocationTargetException;
import java.security.ProtectionDomain;
import org.junit.Test;
import org.objectweb.asm.tree.MethodNode;

/**
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
              public void onStart(int methodId, Class clazz, Object instance) {
                assertEquals(clazz.getName(), TestClass.class.getName());
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
              public void onStart(int methodId, Class clazz, Object instance) {
                assertEquals(clazz.getName(), TestClass.class.getName());
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
  public void testMutableStartLong() throws Exception {
    final StringBuilder steps = new StringBuilder();
    final long aLong = System.currentTimeMillis();
    Class clazz = getInstrumentClass(TestClass.class, new Hook[]{
        new Hook() {
          @Override
          public Filter getFilter() {
            return new Filter() {
              @Override
              public boolean instrumentClass(String className, ProtectionDomain protectionDomain,
                  ClassLoader cl) {
                return true;
              }

              @Override
              public boolean instrumentMethod(UnloadedClassInfo classInfo, MethodNode mn) {
                return mn.name.equals("getLong");
              }
            };
          }

          @Override
          public Listener getListener() {
            return new StartMutableListener() {

              @Override
              public Return onStart(int methodId, Class clazz, Object instance, Object[] args) {
                assertEquals(clazz.getName(), TestClass.class.getName());
                steps.append("1");
                return new Return(aLong);
              }
            };
          }
        }
    });
    Long ret = (Long) clazz.getMethod("getLong").invoke(null);
    assertEquals(aLong, ret.longValue());
    System.out.println(clazz.getClassLoader());
    assertEquals("1", steps.toString());
  }

  @Test
  public void testMutableStartRef() throws Exception {
    final StringBuilder steps = new StringBuilder();
    final Object aObject = "5";
    Class clazz = getInstrumentClass(TestClass.class, new Hook[]{
        new Hook() {
          @Override
          public Filter getFilter() {
            return new Filter() {
              @Override
              public boolean instrumentClass(String className, ProtectionDomain protectionDomain,
                  ClassLoader cl) {
                return true;
              }

              @Override
              public boolean instrumentMethod(UnloadedClassInfo classInfo, MethodNode mn) {
                return mn.name.equals("getObject");
              }
            };
          }

          @Override
          public Listener getListener() {
            return new StartMutableListener() {

              @Override
              public Return onStart(int methodId, Class clazz, Object instance, Object[] args) {
                assertEquals(clazz.getName(), TestClass.class.getName());
                steps.append("1");
                return new Return(aObject);
              }
            };
          }
        }
    });
    Object ret = (Object) clazz.getMethod("getObject").invoke(null);
    assertEquals(aObject, ret);
    System.out.println(clazz.getClassLoader());
    assertEquals("1", steps.toString());
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
              public void onFinishedThrowable(int methodId, Class clazz, Object instance,
                  Throwable th) {
                assertEquals(clazz.getName(), TestClass.class.getName());
                assertTrue(th instanceof TestRuntimeException);
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
              public void onBeforeThrown(int methodId, Class clazz, Object instance, Throwable th) {
                assertEquals(clazz.getName(), TestClass.class.getName());
                assertTrue(th instanceof TestRuntimeException);
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
              public void onBeforeThrown(int methodId, Class clazz, Object instance, Throwable th) {
                assertEquals(clazz.getName(), TestClass.class.getName());
                assertTrue(th instanceof TestRuntimeException);
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
              public void onBeforeThrown(int methodId, Class clazz, Object instance, Throwable th) {
                assertEquals(clazz.getName(), TestClass.class.getName());
                assertTrue(th instanceof TestRuntimeException);
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
            public void onFinishedReturn(int methodId, Class clazz, Object instance, Object ret) {
              assertNotNull(ret);
              assertEquals(clazz.getName(), TestClass.class.getName());
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
          return new FinishReturnArgumentsListener() {
            @Override
            public void onFinishedReturn(int methodId, Class clazz, Object instance, Object[] args, Object ret) {
              assertNotNull(ret);
              assertEquals(clazz.getName(), TestClass.class.getName());
              steps.append("2");
            }
          };
        }
      }
    });
    clazz.getMethod("getLong").invoke(null);
    clazz.getMethod("getInt").invoke(null);
    clazz.getMethod("getObject").invoke(null);
    assertEquals(steps.toString(), "121212");
  }

// TODO uncoment when #2 is fixed

//  public void testVoidReturn() throws Exception {
//    final StringBuilder steps = new StringBuilder();
//    Class clazz = getInstrumentClass(TestClass.class, new Hook[]{
//      new Hook() {
//        @Override
//        public Filter getFilter() {
//          return new AllFilter();
//        }
//
//        @Override
//        public Listener getListener() {
//          return new FinishReturnListener() {
//            @Override
//            public void onFinishedReturn(int methodId, Object instance, Object ret) {
//              assertNull(ret);
//             
//              steps.append("1");
//            }
//          };
//        }
//      },
//      new Hook() {
//        @Override
//        public Filter getFilter() {
//          return new AllFilter();
//        }
//
//        @Override
//        public Listener getListener() {
//          return new FinishThrowableListener() {
//            @Override
//            public void onFinishedThrowable(int methodId, Object instance, Throwable th) {
//              assertTrue("This should not called", false);
//            }
//          };
//        }
//      },
//      new Hook() {
//        @Override
//        public Filter getFilter() {
//          return new AllFilter();
//        }
//
//        @Override
//        public Listener getListener() {
//          return new FinishReturnListener() {
//            @Override
//            public void onFinishedReturn(int methodId, Object instance, Object ret) {
//              assertNull(ret);
//             
//              steps.append("2");
//            }
//          };
//        }
//      }
//    });
//    clazz.getMethod("execVoid").invoke(null);
//    assertEquals("21", steps.toString());
//  }
//
//
//  @Test
//  public void testUncaughtThrowable() throws Exception {
//    final StringBuilder steps = new StringBuilder();
//    Class clazz = getInstrumentClass(TestClass.class, new Hook[]{
//      new Hook() {
//        @Override
//        public Filter getFilter() {
//          return new AllFilter();
//        }
//
//        @Override
//        public Listener getListener() {
//          return new FinishReturnListener() {
//            @Override
//            public void onFinishedReturn(int methodId, Object instance, Object ret) {
//              assertNotNull(ret);
//             
//              steps.append("1r");
//            }
//          };
//        }
//      },
//      new Hook() {
//        @Override
//        public Filter getFilter() {
//          return new AllFilter();
//        }
//
//        @Override
//        public Listener getListener() {
//          return new FinishThrowableListener() {
//            @Override
//            public void onFinishedThrowable(int methodId, Object instance, Throwable th) {
//              assertNotNull(th);
//             
//              steps.append("1t");
//            }
//          };
//        }
//      },
//      new Hook() {
//        @Override
//        public Filter getFilter() {
//          return new AllFilter() {
//            @Override
//            public boolean instrumentMethod(ClassNode classNode, MethodNode mn) {
//              return mn.name.equals("getLongWithConditionalException");
//            }
//          };
//        }
//
//        @Override
//        public Listener getListener() {
//          return new FinishThrowableListener() {
//            @Override
//            public void onFinishedThrowable(int methodId, Object instance, Throwable th) {
//              assertNotNull(th);
//             
//              steps.append("2t");
//            }
//          };
//        }
//      },
//      new Hook() {
//        @Override
//        public Filter getFilter() {
//          return new AllFilter() {
//            @Override
//            public boolean instrumentMethod(ClassNode classNode, MethodNode mn) {
//              return mn.name.equals("getLongWithConditionalException");
//            }
//          };
//        }
//
//        @Override
//        public Listener getListener() {
//          return new FinishReturnListener() {
//            @Override
//            public void onFinishedReturn(int methodId, Object instance, Object ret) {
//              assertNotNull(ret);
//             
//              steps.append("2r");
//            }
//          };
//        }
//      }
//    });
//    clazz.getMethod("getLongWithConditionalException", boolean.class).invoke(null, false);
//    assertEquals("2r1r", steps.toString());
//    steps.setLength(0);
//    boolean captured = false;
//    try {
//      clazz.getMethod("getLongWithConditionalException", boolean.class).invoke(null, true);
//      assertEquals(steps.toString(), "");
//    } catch (InvocationTargetException ite) {
//      if (ite.getTargetException() instanceof TestRuntimeException) {
//        captured = true;
//        assertEquals("1t2t1t", steps.toString());
//      }
//    }
//    assertTrue("Expected exception", captured);
//  }

}
