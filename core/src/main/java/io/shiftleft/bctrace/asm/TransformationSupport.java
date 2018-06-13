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

import io.shiftleft.bctrace.Bctrace;
import io.shiftleft.bctrace.Init;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Scanner;

/**
 * @author Ignacio del Valle Alles idelvall@shiftleft.io
 */
public class TransformationSupport {

  private static final String IGNORE_LIST_DESCRIPTOR_NAME = "bctrace.ignore";
  private static final String[] CLASSNAME_PREFIX_IGNORE_LIST = readIgnoreClassNamesFromDescriptors();

  private static String[] readIgnoreClassNamesFromDescriptors() {
    try {
      ClassLoader cl = Init.class.getClassLoader();
      if (cl == null) {
        cl = ClassLoader.getSystemClassLoader().getParent();
      }
      Enumeration<URL> resources = cl.getResources(IGNORE_LIST_DESCRIPTOR_NAME);
      ArrayList<String> list = new ArrayList<String>();
      while (resources.hasMoreElements()) {
        URL url = resources.nextElement();
        Scanner scanner = new Scanner(url.openStream());
        while (scanner.hasNextLine()) {
          String line = scanner.nextLine().trim();
          if (!line.isEmpty()) {
            list.add(line);
          }
        }
      }
      return list.toArray(new String[list.size()]);
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }

  public static boolean isTransformable(String jvmClassName, ClassLoader loader) {

    if (jvmClassName == null) {
      return false;
    }
    if (jvmClassName.contains("$$Lambda$")) {
      return false;
    }
    for (int i = 0; i < CLASSNAME_PREFIX_IGNORE_LIST.length; i++) {
      String prefix = CLASSNAME_PREFIX_IGNORE_LIST[i];
      if (jvmClassName.startsWith(prefix)) {
        return false;
      }
    }
    if (loader != null && loader == Bctrace.class.getClassLoader()) {
      return false;
    }
    return true;
  }

  public static boolean isRetransformable(Class clazz) {
    if (clazz.isInterface() || clazz.isPrimitive() || clazz.isArray()) {
      return false;
    }
    return isTransformable(clazz.getName().replace('.', '/'), clazz.getClassLoader());
  }
}
