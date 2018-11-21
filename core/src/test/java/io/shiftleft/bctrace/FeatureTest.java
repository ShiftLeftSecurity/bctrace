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
import io.shiftleft.bctrace.hierarchy.BctraceClass;
import io.shiftleft.bctrace.hook.direct.CallSiteHook;
import io.shiftleft.bctrace.hook.generic.GenericHook;
import io.shiftleft.bctrace.hook.Hook;
import io.shiftleft.bctrace.hook.direct.MethodHook;
import io.shiftleft.bctrace.runtime.listener.generic.BeforeThrownListener;
import io.shiftleft.bctrace.runtime.listener.generic.Disabled;
import io.shiftleft.bctrace.runtime.listener.generic.FinishListener;
import io.shiftleft.bctrace.runtime.listener.generic.GenericListener;
import io.shiftleft.bctrace.runtime.listener.generic.StartListener;
import io.shiftleft.bctrace.runtime.listener.direct.CallSiteListener;
import io.shiftleft.bctrace.runtime.listener.direct.DirectListener;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
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
              public Object onFinish(@Disabled int methodId, @Disabled Class clazz,
                  @Disabled Object instance, @Disabled Object[] args,
                  Object ret, Throwable th) {
                if (clazz == null && args == null) {
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
  public void testStartDisabled() throws Exception {
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
              public void onStart(@Disabled int methodId, @Disabled Class clazz,
                  @Disabled Object instance, @Disabled Object[] args) {
                if (clazz == null && args == null) {
                  steps.append("1");
                }
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

  public static interface CallSiteListener1Interface {

    public void onBeforeCall(Class clazz, Object instance,
        Object callSiteInstance, Object src, int srcPos,
        Object dest, int destPos,
        int length);
  }

  public static class CallSiteListener1 extends CallSiteListener implements
      CallSiteListener1Interface {

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

  public static interface CallSiteListener2Interface {

    public void onBeforeCall(Class clazz, Object instance,
        Object callSiteInstance, Object src, int srcPos,
        Object dest, int destPos,
        int length);
  }


  public static class CallSiteListener2 extends CallSiteListener implements
      CallSiteListener2Interface {

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

  public static interface DirectListener1Interface {

    public void onBeforeCall(Class clazz, Object instance, String[] array1, String[] array2);
  }

  public static class DirectListener1 extends DirectListener implements DirectListener1Interface {

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

  public static interface DirectListener2Interface extends DirectListener1Interface {

  }

  public static class DirectListener2 extends DirectListener implements DirectListener2Interface {

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

  public static interface DirectListener3Interface {

    public String[] onFinish(Class clazz, Object instance, String[] array1,
        String[] array2,
        String[] ret);
  }

  public static class DirectListener3 extends DirectListener implements DirectListener3Interface {

    private final StringBuilder sb;

    public DirectListener3(StringBuilder sb) {
      this.sb = sb;
    }

    @ListenerMethod(type = ListenerType.onFinish)
    public String[] onFinish(Class clazz, Object instance, String[] array1,
        String[] array2,
        String[] ret) {
      sb.append("1");
      assertEquals(clazz.getName(), TestClass.class.getName());
      assertNull(instance);
      assertNotNull(array1);
      assertNotNull(array2);
      assertTrue(array1.length + array2.length == ret.length);

      return ret;
    }
  }

  public static interface DirectListener4Interface {

    public String[] onFinish(Class clazz, Object instance, String[] array1,
        String[] array2,
        String[] ret);
  }

  public static class DirectListener4 extends DirectListener implements DirectListener4Interface {

    private final StringBuilder sb;

    public DirectListener4(StringBuilder sb) {
      this.sb = sb;
    }

    @ListenerMethod(type = ListenerType.onFinish)
    public String[] onFinish(Class clazz, Object instance, String[] array1,
        String[] array2,
        String[] ret) {
      sb.append("2");
      assertEquals(clazz.getName(), TestClass.class.getName());
      assertNull(instance);
      assertNotNull(array1);
      assertNotNull(array2);
      assertTrue(array1.length + array2.length == ret.length);
      return new String[]{"1", "2", "3", "4"};
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
    String[] ret = (String[]) clazz
        .getMethod("concatenateStringArrays", String[].class, String[].class).invoke(null, s1, s2);
    assertEquals(Arrays.toString(ret), Arrays.toString(new String[]{"1", "2", "3", "4"}));
    assertEquals("21", sb.toString());
  }

  public static interface DirectListenerVoidInterface {

    public void onFinish(Class clazz, Object instance);
  }

  public static class DirectListenerVoid extends DirectListener implements
      DirectListenerVoidInterface {

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
              public void onBeforeThrow(int methodId, Class clazz, Object instance, Object[] args,
                  Throwable th) {
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
              public void onBeforeThrow(int methodId, Class clazz, Object instance, Object[] args,
                  Throwable th) {
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
  public void testFinishReturn() throws Exception {
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
                  @Disabled Object ret, Throwable th) {
                assertNull(ret);
                steps.append("1");
                return null;
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
                  @Disabled Object ret, Throwable th) {
                assertNull(ret);
                steps.append("2");
                return null;
              }
            };
          }
        }
    });
    Object ret = clazz.getMethod("getLong").invoke(null);
    Long value = (Long) ret;
    assertEquals((long) value, 2l);
    assertEquals(steps.toString(), "21");
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
                  Object ret, @Disabled Throwable th) {
                steps.append("1");
                assertNull(th);
                return null;
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
