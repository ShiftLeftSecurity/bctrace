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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import io.shiftleft.bctrace.TestClass.TestRuntimeException;
import io.shiftleft.bctrace.filter.AllFilter;
import io.shiftleft.bctrace.filter.Filter;
import io.shiftleft.bctrace.filter.MethodFilter;
import io.shiftleft.bctrace.hook.CallSiteHook;
import io.shiftleft.bctrace.hook.GenericHook;
import io.shiftleft.bctrace.hook.Hook;
import io.shiftleft.bctrace.hook.MethodHook;
import io.shiftleft.bctrace.runtime.listener.generic.BeforeThrownListener;
import io.shiftleft.bctrace.runtime.listener.generic.GenericListener;
import io.shiftleft.bctrace.runtime.listener.generic.ReturnListener;
import io.shiftleft.bctrace.runtime.listener.generic.StartListener;
import io.shiftleft.bctrace.runtime.listener.specific.CallSiteListener;
import io.shiftleft.bctrace.runtime.listener.specific.DirectListener;
import java.lang.reflect.InvocationTargetException;
import org.junit.Test;

/**
 * @author Ignacio del Valle Alles idelvall@shiftleft.io
 */
public class FeatureTest extends BcTraceTest {

  @Test
  public void testStart() throws Exception {
    final StringBuilder steps = new StringBuilder();
    Class clazz = getInstrumentClass(TestClass.class, new Hook[]{
        new GenericHook() {
          @Override
          public Filter getFilter() {
            return new AllFilter();
          }

          @Override
          public GenericListener getListener() {
            return new StartListener() {
              @Override
              public void onStart(int methodId, Class clazz, Object instance, Object[] args) {
                assertEquals(clazz.getName(), TestClass.class.getName());
                steps.append("1");
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
            return new StartListener() {

              @Override
              public void onStart(int methodId, Class clazz, Object instance, Object[] args) {
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
            return new ReturnListener() {
              @Override
              public void onReturn(int methodId, Class clazz, Object instance, Object[] args,
                  Object ret) {
                assertEquals(clazz.getName(), TestClass.class.getName());
                steps.append("1");
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
            return new ReturnListener() {
              @Override
              public void onReturn(int methodId, Class clazz, Object instance, Object[] args,
                  Object ret) {
                assertEquals(clazz.getName(), TestClass.class.getName());
                steps.append("2");
              }
            };
          }
        }
    });
    clazz.getMethod("execVoid").invoke(null);
    System.out.println(clazz.getClassLoader());
    assertEquals("21", steps.toString());
  }

  public static class CallSiteListener1 extends CallSiteListener {

    private final StringBuilder sb;

    public CallSiteListener1(StringBuilder sb) {
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

    @ListenerMethod(type = ListenerType.onBeforeCall)
    public void onBeforeCall(Class clazz, Object instance,
        Object callSiteInstance, Object src, int srcPos,
        Object dest, int destPos,
        int length) {
      sb.append("1");
      assertEquals(clazz.getName(), TestClass.class.getName());
      assertEquals(instance.getClass().getName(), TestClass.class.getName());
      assertTrue(callSiteInstance == null);
      assertNotNull(src);
      assertNotNull(dest);
    }
  }

  public static class CallSiteListener2 extends CallSiteListener {

    private final StringBuilder sb;

    public CallSiteListener2(StringBuilder sb) {
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

    @ListenerMethod(type = ListenerType.onBeforeCall)
    public void onBeforeCall(Class clazz, Object instance,
        Object callSiteInstance, Object src, int srcPos,
        Object dest, int destPos,
        int length) {
      sb.append("2");
      assertEquals(clazz.getName(), TestClass.class.getName());
      assertEquals(instance.getClass().getName(), TestClass.class.getName());
      assertTrue(callSiteInstance == null);
      assertNotNull(src);
      assertNotNull(dest);
    }
  }

  @Test
  public void testCallSite() throws Exception {
    StringBuilder sb = new StringBuilder();
    CallSiteListener1 callSiteListener1 = new CallSiteListener1(sb);
    CallSiteListener2 callSiteListener2 = new CallSiteListener2(sb);
    Class clazz = getInstrumentClass(TestClass.class, new Hook[]{
        new CallSiteHook(new AllFilter(), callSiteListener1),
        new CallSiteHook(new AllFilter(), callSiteListener2)
    }, false);

    clazz.getMethod("arrayCopyWrapper2").invoke(null);
    assertEquals("12", sb.toString());
  }

  public static class DirectListener1 extends DirectListener {

    private final StringBuilder sb;

    public DirectListener1(StringBuilder sb) {
      this.sb = sb;
    }

    @ListenerMethod(type = ListenerType.onStart)
    public void onBeforeCall(Class clazz, Object instance, String[] array1, String[] array2) {
      sb.append("1");
      assertEquals(clazz.getName(), TestClass.class.getName());
      assertNull(instance);
      assertNotNull(array1);
      assertNotNull(array2);
    }
  }

  public static class DirectListener2 extends DirectListener {

    private final StringBuilder sb;

    public DirectListener2(StringBuilder sb) {
      this.sb = sb;
    }

    @ListenerMethod(type = ListenerType.onStart)
    public void onBeforeCall(Class clazz, Object instance, String[] array1, String[] array2) {
      sb.append("2");
      assertEquals(clazz.getName(), TestClass.class.getName());
      assertNull(instance);
      assertNotNull(array1);
      assertNotNull(array2);
    }
  }

  @Test
  public void testDirectStart() throws Exception {
    StringBuilder sb = new StringBuilder();
    DirectListener listener1 = new DirectListener1(sb);
    DirectListener listener2 = new DirectListener2(sb);
    Class clazz = getInstrumentClass(TestClass.class, new Hook[]{
        new MethodHook(new MethodFilter("io/shiftleft/bctrace/TestClass", "concatenateStringArrays",
            "([Ljava/lang/String;[Ljava/lang/String;)[Ljava/lang/String;"), listener1),
        new MethodHook(new MethodFilter("io/shiftleft/bctrace/TestClass", "concatenateStringArrays",
            "([Ljava/lang/String;[Ljava/lang/String;)[Ljava/lang/String;"), listener2)
    }, false);

    String[] s1 = {"a", "b"};
    String[] s2 = {"c", "d"};
    clazz.getMethod("concatenateStringArrays", String[].class, String[].class).invoke(null, s1, s2);
    assertEquals("12", sb.toString());
  }

  public static class DirectListener3 extends DirectListener {

    private final StringBuilder sb;

    public DirectListener3(StringBuilder sb) {
      this.sb = sb;
    }

    @ListenerMethod(type = ListenerType.onFinish)
    public void onFinish(Class clazz, Object instance, String[] array1, String[] array2,
        String[] ret) {
      sb.append("1");
      assertEquals(clazz.getName(), TestClass.class.getName());
      assertNull(instance);
      assertNotNull(array1);
      assertNotNull(array2);
      assertTrue(array1.length + array2.length == ret.length);
    }
  }

  public static class DirectListener4 extends DirectListener {

    private final StringBuilder sb;

    public DirectListener4(StringBuilder sb) {
      this.sb = sb;
    }

    @ListenerMethod(type = ListenerType.onFinish)
    public void onFinish(Class clazz, Object instance, String[] array1, String[] array2,
        String[] ret) {
      sb.append("2");
      assertEquals(clazz.getName(), TestClass.class.getName());
      assertNull(instance);
      assertNotNull(array1);
      assertNotNull(array2);
      assertTrue(array1.length + array2.length == ret.length);
    }
  }

  @Test
  public void testDirectReturn() throws Exception {
    StringBuilder sb = new StringBuilder();
    DirectListener listener3 = new DirectListener3(sb);
    DirectListener listener4 = new DirectListener4(sb);
    Class clazz = getInstrumentClass(TestClass.class, new Hook[]{
        new MethodHook(new MethodFilter("io/shiftleft/bctrace/TestClass", "concatenateStringArrays",
            "([Ljava/lang/String;[Ljava/lang/String;)[Ljava/lang/String;"), listener3),
        new MethodHook(new MethodFilter("io/shiftleft/bctrace/TestClass", "concatenateStringArrays",
            "([Ljava/lang/String;[Ljava/lang/String;)[Ljava/lang/String;"), listener4)
    }, false);

    String[] s1 = {"a", "b"};
    String[] s2 = {"c", "d"};
    clazz.getMethod("concatenateStringArrays", String[].class, String[].class).invoke(null, s1, s2);
    assertEquals("21", sb.toString());
  }

  public static class DirectListenerVoid extends DirectListener {

    private final StringBuilder sb;

    public DirectListenerVoid(StringBuilder sb) {
      this.sb = sb;
    }

    @ListenerMethod(type = ListenerType.onFinish)
    public void onFinish(Class clazz, Object instance) {
      sb.append("1");
      assertEquals(clazz.getName(), TestClass.class.getName());
      assertNull(instance);
    }
  }

  @Test
  public void testDirectReturnVoid() throws Exception {
    StringBuilder sb = new StringBuilder();
    DirectListener listener = new DirectListenerVoid(sb);
    Class clazz = getInstrumentClass(TestClass.class, new Hook[]{
        new MethodHook(new MethodFilter("io/shiftleft/bctrace/TestClass", "doFrames",
            "()V"), listener)
    }, false);
    clazz.getMethod("doFrames").invoke(null);
    assertEquals("1", sb.toString());
  }

  public static class AfterCallSiteListener extends CallSiteListener {

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
    public void onBeforeCall(Class clazz, Object instance,
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
  public void testAfterCallSite() throws Exception {
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

  @Test
  public void testThrown() throws Exception {
    final StringBuilder steps = new StringBuilder();
    Class clazz = getInstrumentClass(TestClass.class, new Hook[]{
        new GenericHook() {
          @Override
          public Filter getFilter() {
            return new AllFilter();
          }

          @Override
          public GenericListener getListener() {
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
        new GenericHook() {
          @Override
          public Filter getFilter() {
            return new AllFilter();
          }

          @Override
          public GenericListener getListener() {
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
//          return new ReturnListener() {
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
//          return new ReturnListener() {
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
//          return new ReturnListener() {
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
//          return new ReturnListener() {
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
