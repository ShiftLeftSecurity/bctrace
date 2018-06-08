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

import io.shiftleft.bctrace.asm.util.ASMUtils;
import java.io.InputStream;

/**
 * @author Ignacio del Valle Alles idelvall@shiftleft.io
 */
public class TestClass {

  public TestClass() {
  }

  public TestClass(int i) {
    throw new TestRuntimeException("This constructor throws a Runtime Exception");
  }

  public void arrayCopyWrapper() {
    char[] a = new char[]{'a', 'b', 'c'};
    char[] b = new char[2];
    System.arraycopy(a, 0, b, 0, 2);
  }

  public static void staticArrayCopyWrapper() {
    char[] a = new char[]{'a', 'b', 'c'};
    char[] b = new char[2];
    System.arraycopy(a, 0, b, 0, 2);
  }

  public static void arrayCopyWrapper2() {
    TestClass t = new TestClass();
    t.arrayCopyWrapper();
  }

  public static long fact(long n) {
    if (n == 1) {
      return 1;
    } else {
      return fact(n - 1) * n;
    }
  }

  public static long factWrapper(Long n) {
    if (n == 1) {
      return 1;
    } else {
      return factWrapper(n - 1) * n;
    }
  }

  public static void throwRuntimeException() {
    throw new TestRuntimeException("A testing runtime exception");
  }

  public static long getLongWithConditionalException(boolean throwException) {
    if (throwException) {
      throwRuntimeException();
    }
    return 1;
  }

  public static boolean isEmpty(Object[] array) {
    return (array == null || array.length == 0);
  }

//  public static String[] concatenateStringArrays(String[] array1, String[] array2) {
//    if (isEmpty(array1)) {
//      return array2;
//    }
//    if (isEmpty(array2)) {
//      return array1;
//    }
//    String[] newArr = new String[array1.length + array2.length];
//    System.arraycopy(array1, 0, newArr, 0, array1.length);
//    System.arraycopy(array2, 0, newArr, array1.length, array2.length);
//    return newArr;
//  }

  public static void doFrames() {
    Number num = (System.currentTimeMillis() % 2 == 0 ? new Integer(3) : new Long(4));
    System.out.println(num.intValue());
  }

  public static long getLong() {
    return 2;
  }

  public static int getInt() {
    return 2;
  }

  public static Object getObject() {
    return "2";
  }

  public static void execVoid() {
  }

  public static void main(String[] args) throws Exception {
    Class clazz = TestClass.class;
    String className = clazz.getCanonicalName();
    String resourceName = className.replace('.', '/') + ".class";
    InputStream is = clazz.getClassLoader().getResourceAsStream(resourceName);
    byte[] bytes = ASMUtils.toByteArray(is);
    BcTraceTest.viewByteCode(bytes);
  }

  public static class TestRuntimeException extends RuntimeException {

    public TestRuntimeException(String message) {
      super(message);
    }
  }
}
