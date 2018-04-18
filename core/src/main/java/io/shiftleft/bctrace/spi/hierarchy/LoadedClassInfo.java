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

import java.security.ProtectionDomain;

/**
 * @author Ignacio del Valle Alles idelvall@shiftleft.io
 */
public class LoadedClassInfo extends HierarchyClassInfo {

  private final Class clazz;
  private final HierarchyClassInfo superClass;
  private final HierarchyClassInfo[] interfaces;

  LoadedClassInfo(Class clazz) {
    if (clazz == null) {
      throw new IllegalArgumentException("Class instance is required");
    }
    this.clazz = clazz;
    if (clazz.getSuperclass() == null) {
      this.superClass = null;
    } else {
      this.superClass = new LoadedClassInfo(clazz.getSuperclass());
    }
    Class[] interfaces = clazz.getInterfaces();
    this.interfaces = new HierarchyClassInfo[interfaces.length];
    int i = 0;
    for (Class cl : interfaces) {
      this.interfaces[i] = new LoadedClassInfo(cl);
      i++;
    }
  }

  @Override
  public HierarchyClassInfo getSuperClass() {
    return this.superClass;
  }

  @Override
  public HierarchyClassInfo[] getInterfaces() {
    return this.interfaces;
  }

  @Override
  public String getName() {
    return clazz.getName();
  }

  @Override
  public ClassLoader getClassLoader() {
    return this.clazz.getClassLoader();
  }

  @Override
  public ProtectionDomain getProtectionDomain() {
    return this.clazz.getProtectionDomain();
  }

  @Override
  public boolean isInterface() {
    return this.clazz.isInterface();
  }

  public Class getClazz() {
    return clazz;
  }

}
