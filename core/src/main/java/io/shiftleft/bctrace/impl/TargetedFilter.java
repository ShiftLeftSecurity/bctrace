package io.shiftleft.bctrace.impl;

import io.shiftleft.bctrace.spi.Filter;
import io.shiftleft.bctrace.spi.hierarchy.BctraceClass;
import org.objectweb.asm.tree.MethodNode;

public class TargetedFilter extends Filter {

  private final String className;
  private final String methodName;
  private final String methodDescriptor;

  public TargetedFilter(String className, String methodName, String methodDescriptor) {
    this.className = className;
    this.methodName = methodName;
    this.methodDescriptor = methodDescriptor;
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
  public boolean instrumentMethod(BctraceClass bctraceClass, MethodNode mn) {
    return this.methodName.equals(mn.name) && this.methodDescriptor.equals(mn.desc);
  }
}
