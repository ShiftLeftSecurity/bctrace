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
package io.shiftleft.bctrace.asm;

import io.shiftleft.bctrace.MethodRegistry;
import io.shiftleft.bctrace.asm.util.ASMUtils;
import io.shiftleft.bctrace.util.collections.IntObjectHashMap;
import io.shiftleft.bctrace.util.collections.IntObjectHashMap.EntryVisitor;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Ignacio del Valle Alles idelvall@shiftleft.io
 */
public final class MethodRegistryImpl extends MethodRegistry {

  private final ReferenceQueue<ClassLoader> classLoaderReferenceQueue = new ReferenceQueue<ClassLoader>();
  private final AtomicInteger counter = new AtomicInteger(1);
  private final IntObjectHashMap<MethodInfoImpl> map1 = new IntObjectHashMap<MethodInfoImpl>(100);

  private static final MethodRegistryImpl INSTANCE = new MethodRegistryImpl();

  public static MethodRegistryImpl getInstance() {
    return INSTANCE;
  }

  private MethodRegistryImpl() {
  }

  @Override
  public synchronized MethodInfo getMethod(int methodId) {
    return map1.get(methodId);
  }

  @Override
  public synchronized int size() {
    return map1.size();
  }

  synchronized int add(MethodInfo mi) {
    int methodId = counter.getAndIncrement();
    map1.put(methodId, (MethodInfoImpl) mi);
    return methodId;
  }

  /**
   * For each classloader about to be garbage collected, this method removes all entries in the
   * registry belonging to it. Running time is O(g*n), g: number of classloaders GC-ed (tipically
   * 1), n: number of entries in the registry
   */
  private synchronized void expungeStaleEntries() {
    for (Reference<? extends ClassLoader> ref;
        (ref = classLoaderReferenceQueue.poll()) != null; ) {
      final Reference cRef = ref;
      map1.visitEntries(new EntryVisitor<MethodInfoImpl>() {
        @Override
        public boolean remove(int key, MethodInfoImpl value) {
          return value.classLoaderRef == cRef;
        }
      });
    }
  }

  synchronized MethodInfoImpl remove(int methodId) {
    return map1.remove(methodId);
  }

  MethodInfo newMethodInfo(ClassLoader classLoader, String jvmClassName, String methodName,
      String methodDescriptor, int modifiers) {

    return new MethodInfoImpl(
        new WeakReference<ClassLoader>(classLoader, this.classLoaderReferenceQueue),
        jvmClassName,
        methodName,
        methodDescriptor,
        modifiers);
  }

  private static final class MethodInfoImpl implements MethodInfo {

    private final WeakReference<ClassLoader> classLoaderRef;
    private final int classLoaderHashCode;
    private final String jvmClassName;
    private final String methodName;
    private final String methodDescriptor;
    private final int modifiers;

    private String packageName;

    private MethodInfoImpl(WeakReference<ClassLoader> classLoaderRef, String jvmClassName,
        String methodName,
        String methodDescriptor, int modifiers) {
      if (classLoaderRef == null || classLoaderRef.get() == null) {
        this.classLoaderHashCode = 0;
        this.classLoaderRef = null;
      } else {
        this.classLoaderHashCode = classLoaderRef.get().hashCode();
        this.classLoaderRef = classLoaderRef;
      }
      this.jvmClassName = jvmClassName;
      this.methodName = methodName;
      this.methodDescriptor = methodDescriptor;
      this.modifiers = modifiers;
    }

    @Override
    public String getJvmClassName() {
      return jvmClassName;
    }

    @Override
    public String getMethodName() {
      return this.methodName;
    }

    @Override
    public String getMethodDescriptor() {
      return this.methodDescriptor;
    }

    @Override
    public int getModifiers() {
      return modifiers;
    }

    @Override
    public String getPackageName() {
      if (packageName == null) {
        packageName = this.jvmClassName.substring(0, this.jvmClassName.lastIndexOf("/"))
            .replace('/', '.');
      }
      return this.packageName;
    }

    @Override
    public boolean isAbstract() {
      return ASMUtils.isAbstract(this.modifiers);
    }

    @Override
    public boolean isNative() {
      return ASMUtils.isNative(this.modifiers);
    }

    @Override
    public boolean isStatic() {
      return ASMUtils.isStatic(this.modifiers);
    }

    @Override
    public boolean isPublic() {
      return ASMUtils.isPublic(this.modifiers);
    }

    @Override
    public boolean isProtected() {
      return ASMUtils.isProtected(this.modifiers);
    }

    @Override
    public boolean isDefault() {
      return !isPublic() && !isProtected() && !isPrivate();
    }

    @Override
    public boolean isPrivate() {
      return ASMUtils.isPrivate(this.modifiers);
    }

    @Override
    public String toString() {
      return jvmClassName + "." + methodName + methodDescriptor;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      MethodInfoImpl that = (MethodInfoImpl) o;
      if (classLoaderRef == null) {
        if (that.classLoaderRef != null) {
          return false;
        }
      } else {
        if (that.classLoaderRef == null) {
          return false;
        } else if (classLoaderRef.get() != that.classLoaderRef.get()) {
          return false;
        }
      }
      if (!jvmClassName.equals(that.jvmClassName)) {
        return false;
      }
      if (!methodName.equals(that.methodName)) {
        return false;
      }
      return methodDescriptor.equals(that.methodDescriptor);
    }

    @Override
    public int hashCode() {
      int result = classLoaderHashCode;
      result = 31 * result + jvmClassName.hashCode();
      result = 31 * result + methodName.hashCode();
      result = 31 * result + methodDescriptor.hashCode();
      return result;
    }
  }
}
