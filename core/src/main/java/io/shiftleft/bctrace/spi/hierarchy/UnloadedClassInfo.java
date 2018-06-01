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

import io.shiftleft.bctrace.asm.util.ASMUtils;
import io.shiftleft.bctrace.Bctrace;
import java.net.URL;
import java.util.List;
import org.objectweb.asm.tree.ClassNode;

/**
 * @author Ignacio del Valle Alles idelvall@shiftleft.io
 */
public class UnloadedClassInfo extends HierarchyClassInfo {

  private final ClassNode cn;
  private final ClassLoader cl;
  private final URL codeSource;
  private final HierarchyClassInfo superClass;
  private final HierarchyClassInfo[] interfaces;

  public UnloadedClassInfo(ClassNode cn, URL codeSource, ClassLoader cl, Bctrace bctrace) {
    this.cn = cn;
    this.cl = cl;
    this.codeSource = codeSource;
    if (this.cn.superName == null) {
      this.superClass = null;
    } else {
      this.superClass = HierarchyClassInfo.from(this.cn.superName.replace('/', '.'), this.cl, bctrace);
    }
    List<String> ifaces = this.cn.interfaces;
    this.interfaces = new HierarchyClassInfo[ifaces.size()];
    int i = 0;
    for (String interfaceName : ifaces) {
      this.interfaces[i] = HierarchyClassInfo.from(interfaceName.replace('/', '.'), cl, bctrace);
      i++;
    }
  }

  public ClassNode getRawClassNode() {
    return cn;
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
    return cn.name;
  }

  @Override
  public ClassLoader getClassLoader() {
    return this.cl;
  }

  @Override
  public URL getCodeSource() {
    return codeSource;
  }

  @Override
  public boolean isInterface() {
    return ASMUtils.isInterface(this.cn.access);
  }

}

