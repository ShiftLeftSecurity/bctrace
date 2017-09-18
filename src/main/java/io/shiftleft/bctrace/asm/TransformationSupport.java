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
package io.shiftleft.bctrace.asm;

/**
 *
 * @author Ignacio del Valle Alles idelvall@shiftleft.io
 */
public class TransformationSupport {

  private static final String[] CLASSNAME_PREFIX_IGNORE_LIST = new String[]{
    "io/shiftleft/bctrace/",
    "java/lang/Object",
    "java/lang/String",
    "java/lang/ThreadLocal",
    "java/lang/VerifyError",
    "java/lang/instrument",
    "java/lang/invoke",
    "java/lang/ref",
    "java/lang/concurrent",
    "java/security/",
    "sun/",
    "com/sun/",
    "javafx/",
    "oracle/",
  };

  public static boolean isTransformable(String jvmClassName) {

    if (jvmClassName == null) {
      return false;
    }
    if (jvmClassName.contains("$$Lambda$")) {
      return false;
    }
    for (String prefix : CLASSNAME_PREFIX_IGNORE_LIST) {
      if (jvmClassName.startsWith(prefix)) {
        return false;
      }
    }
    return true;
  }

  public static boolean isRetransformable(Class clazz) {
    if (clazz.isInterface() || clazz.isPrimitive() || clazz.isArray()) {
      return false;
    }
    return isTransformable(clazz.getName().replace('.', '/'));
  }
}
