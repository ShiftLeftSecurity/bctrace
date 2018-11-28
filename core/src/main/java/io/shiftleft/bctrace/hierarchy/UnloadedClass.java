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
 * of this source code, which includes information that is confidential and/or proprietary, and
 * is a trade secret, of ShiftLeft, Inc.
 *
 * ANY REPRODUCTION, MODIFICATION, DISTRIBUTION, PUBLIC PERFORMANCE, OR PUBLIC DISPLAY
 * OF OR THROUGH USE OF THIS SOURCE CODE WITHOUT THE EXPRESS WRITTEN CONSENT OF ShiftLeft, Inc.
 * IS STRICTLY PROHIBITED, AND IN VIOLATION OF APPLICABLE LAWS AND INTERNATIONAL TREATIES.
 * THE RECEIPT OR POSSESSION OF THIS SOURCE CODE AND/OR RELATED INFORMATION DOES NOT
 * CONVEY OR IMPLY ANY RIGHTS TO REPRODUCE, DISCLOSE OR DISTRIBUTE ITS
 * CONTENTS, OR TO MANUFACTURE, USE, OR SELL ANYTHING THAT IT MAY DESCRIBE, IN WHOLE OR IN PART.
 */
package io.shiftleft.bctrace.hierarchy;

import io.shiftleft.bctrace.Bctrace;
import io.shiftleft.bctrace.Instrumentation;
import java.io.IOException;
import java.net.URL;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

/**
 * @author Ignacio del Valle Alles idelvall@shiftleft.io
 */
public class UnloadedClass extends BctraceClass {

  private final ClassNode cn;

  UnloadedClass(String name, ClassLoader cl, Instrumentation instrumentation) throws ClassNotFoundException {
    super(name, cl, instrumentation);
    this.cn = createClassNode(getURL());
  }

  public UnloadedClass(String name, ClassLoader cl, ClassNode cn, Instrumentation instrumentation) {
    super(name, cl, instrumentation);
    this.cn = cn;
  }

  private static ClassNode createClassNode(URL url) throws ClassNotFoundException {
    if (url == null) {
      throw new ClassNotFoundException();
    }
    try {
      ClassReader cr = new ClassReader(url.openStream());
      ClassNode cn = new ClassNode();
      cr.accept(cn, 0);
      return cn;
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }

  public ClassNode getClassNode() {
    return cn;
  }

  @Override
  protected String getSuperClassName() {
    if (this.cn.superName == null) {
      return null;
    } else {
      return this.cn.superName.replace('/', '.');
    }
  }

  @Override
  protected String[] getInterfaceNames() {
    String[] ret = new String[this.cn.interfaces.size()];
    for (int i = 0; i < this.cn.interfaces.size(); i++) {
      ret[i] = this.cn.interfaces.get(i).replace('/', '.');
    }
    return ret;
  }

  @Override
  public int getModifiers() {
    return this.cn.access;
  }
}

