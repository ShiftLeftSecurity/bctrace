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
package io.shiftleft.bctrace.filter;

import io.shiftleft.bctrace.hierarchy.BctraceClass;
import io.shiftleft.bctrace.hierarchy.UnloadedClass;
import java.security.ProtectionDomain;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * A filter determines which class methods are instrumented. <br><br> If the class is transformable,
 * the framework performs an initial query to the {@link #acceptClass(String, ProtectionDomain,
 * ClassLoader) acceptClass} method. If this return <code>true</code> the class bytecode is
 * parsed and the filter {@link #acceptClass(UnloadedClass, ProtectionDomain, ClassLoader)
 * acceptMethod} is called. It this other returns true the filter {@link
 * #acceptMethod(UnloadedClass, MethodNode) acceptMethod} method will be invoked once per non
 * abstract nor native method in the class. Invocations returning <code>true</code> lead to a hook
 * insertions into the bytecode of the method.
 *
 * @author Ignacio del Valle Alles idelvall@shiftleft.io
 */
public abstract class MethodFilter extends ClassFilter {

  /**
   * Returns a boolean that determines whether to instrument the specified method
   */
  public abstract boolean acceptMethod(UnloadedClass clazz, MethodNode mn);

  /**
   * A filter that accepts all classes and methods.
   *
   * @author Ignacio del Valle Alles idelvall@shiftleft.io
   */
  public static class AllFilter extends MethodFilter {

    private static final AllFilter INSTANCE = new AllFilter();

    public static AllFilter getInstance() {
      return INSTANCE;
    }

    @Override
    public boolean acceptClass(String className, ProtectionDomain protectionDomain,
        ClassLoader cl) {
      return true;
    }

    @Override
    public boolean acceptMethod(UnloadedClass clazz, MethodNode mn) {
      return true;
    }
  }

  /**
   * Filter that selects a particular class or its child hierarchy
   */
  public final static class DirectMethodFilter extends MethodFilter {

    private final String className;
    private final String methodName;
    private final String methodDescriptor;
    private final boolean virtual;

    public DirectMethodFilter(String className, String methodName, String methodDescriptor) {
      this(className, methodName, methodDescriptor, false);
    }

    public DirectMethodFilter(String className, String methodName, String methodDescriptor,
        boolean virtual) {
      this.className = className;
      this.methodName = methodName;
      this.methodDescriptor = methodDescriptor;
      this.virtual = virtual;
    }

    public String getClassName() {
      return className;
    }

    public String getMethodName() {
      return methodName;
    }

    public String getMethodDescriptor() {
      return methodDescriptor;
    }

    @Override
    public boolean acceptClass(String className, ProtectionDomain protectionDomain,
        ClassLoader cl) {
      if (!virtual) {
        // No virtual and different class name => End filtering
        return this.className.equals(className);
      } else {
        return true;
      }
    }

    @Override
    public boolean acceptClass(UnloadedClass clazz, ProtectionDomain protectionDomain,
        ClassLoader cl) {

      if (!virtual) {
        // Already filtered by name in previous method.
        return true;
      } else {
        // If virtual, check inheritance
        return clazz.isInstanceOf(className.replace('/', '.'));
      }
    }

    @Override
    public boolean acceptMethod(UnloadedClass clazz, MethodNode mn) {
      return this.methodName.equals(mn.name) && this.methodDescriptor.equals(mn.desc);
    }
  }
}
