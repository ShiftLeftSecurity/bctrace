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
package io.shiftleft.bctrace.spi.hierarchy;

import io.shiftleft.bctrace.Bctrace;

/**
 * @author Ignacio del Valle Alles idelvall@shiftleft.io
 */
public class LoadedClass extends BctraceClass {

  private final Class clazz;

  LoadedClass(Class clazz) {
    super(clazz.getName(), clazz.getClassLoader(), null);
    this.clazz = clazz;
  }

  @Override
  protected String getSuperClassName() {
    if (clazz.getSuperclass() == null) {
      return null;
    } else {
      return clazz.getSuperclass().getName();
    }
  }

  @Override
  protected String[] getInterfaceNames() {
    Class[] interfaces = clazz.getInterfaces();
    if (interfaces == null) {
      return null;
    }
    String[] ret = new String[interfaces.length];
    for (int i = 0; i < ret.length; i++) {
      ret[i] = interfaces[i].getName();
    }
    return ret;
  }

  @Override
  public int getModifiers() {
    return this.clazz.getModifiers();
  }

  public Class getClazz() {
    return clazz;
  }

}
