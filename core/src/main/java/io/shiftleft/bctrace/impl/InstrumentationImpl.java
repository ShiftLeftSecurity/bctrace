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
package io.shiftleft.bctrace.impl;

import io.shiftleft.bctrace.asm.TransformationSupport;
import io.shiftleft.bctrace.runtime.DebugInfo;
import io.shiftleft.bctrace.spi.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Single implementation of the {@link Instrumentation Instrumentation} interface.
 *
 * @author Ignacio del Valle Alles idelvall@shiftleft.io
 */
public final class InstrumentationImpl implements Instrumentation {

  private final java.lang.instrument.Instrumentation javaInstrumentation;

  private final Map<String, List<WeakReference<ClassLoader>>> loadedClassesMap = new HashMap<String, List<WeakReference<ClassLoader>>>();
  private final Map<String, List<WeakReference<ClassLoader>>> unModifableLoadedClassesMap = Collections
      .unmodifiableMap(loadedClassesMap);

  private final Map<String, List<WeakReference<ClassLoader>>> transformedClassesMap = new HashMap<String, List<WeakReference<ClassLoader>>>();
  private final Map<String, List<WeakReference<ClassLoader>>> unModifableTransformedClassesMap = Collections
      .unmodifiableMap(loadedClassesMap);


  public InstrumentationImpl(java.lang.instrument.Instrumentation javaInstrumentation) {
    this.javaInstrumentation = javaInstrumentation;
  }

  @Override
  public boolean isRetransformClassesSupported() {
    return javaInstrumentation.isRetransformClassesSupported();
  }

  @Override
  public boolean isModifiableClass(Class<?> clazz) {
    return isRetransformClassesSupported() && TransformationSupport.isRetransformable(clazz)
        && javaInstrumentation.isModifiableClass(clazz);
  }

  @Override
  public boolean isModifiableClass(String jvmClassName) {
    return isRetransformClassesSupported() && TransformationSupport
        .isTransformable(jvmClassName, null);
  }

  public java.lang.instrument.Instrumentation getJavaInstrumentation() {
    return javaInstrumentation;
  }

  @Override
  public void retransformClasses(Class<?>... classes) throws UnmodifiableClassException {
    if (classes != null && classes.length > 0) {
      for (Class<?> clazz : classes) {
        if (!isModifiableClass(clazz)) {
          throw new UnmodifiableClassException(clazz.getName());
        }
        if (DebugInfo.isEnabled()) {
          DebugInfo.getInstance().addRequestedToInstrument(clazz);
        }
      }
      javaInstrumentation.retransformClasses(classes);
    }
  }

  public void addAllLoadedClasses() {
    synchronized (unModifableLoadedClassesMap) {
      if (javaInstrumentation != null) {
        Class[] allLoadedClasses = javaInstrumentation.getAllLoadedClasses();
        for (Class clazz : allLoadedClasses) {
          registerClass(clazz.getName(), clazz.getClassLoader(), loadedClassesMap);
        }
      }
    }
  }

  public void addLoadedClass(String className, ClassLoader cl) {
    synchronized (unModifableLoadedClassesMap) {
      if (loadedClassesMap.isEmpty()) {
        addAllLoadedClasses();
      }
      registerClass(className, cl, loadedClassesMap);
    }
  }

  public void removeLoadedClass(String className, ClassLoader cl) {
    synchronized (unModifableLoadedClassesMap) {
      removeClass(className, cl, loadedClassesMap);
    }
  }

  public void addTransformedClass(String className, ClassLoader cl) {
    synchronized (unModifableTransformedClassesMap) {
      registerClass(className, cl, transformedClassesMap);
    }
  }

  public void removeTransformedClass(String className, ClassLoader cl) {
    synchronized (unModifableTransformedClassesMap) {
      removeClass(className, cl, transformedClassesMap);
    }
  }

  private void registerClass(String className, ClassLoader cl,
      Map<String, List<WeakReference<ClassLoader>>> map) {
    List<WeakReference<ClassLoader>> list = map.get(className);
    if (list == null) {
      list = new LinkedList<WeakReference<ClassLoader>>();
      map.put(className, list);
    } else {
      for (WeakReference<ClassLoader> wr : list) {
        if (cl == null) { // Bootstrap classloader
          if (wr == null) {
            return;
          }
        } else {
          if (wr != null && wr.get() == cl) {
            return;
          }
        }
      }
    }
    if (cl == null) {
      list.add(null);
    } else {
      list.add(new WeakReference<ClassLoader>(cl));
    }
  }

  private synchronized void removeClass(String className, ClassLoader cl,
      Map<String, List<WeakReference<ClassLoader>>> map) {
    LinkedList<WeakReference<ClassLoader>> list = (LinkedList) map.get(className);
    if (list != null) {
      if (cl == null) { // Bootstrap classloader
        list.remove(null);
      } else {
        for (WeakReference<ClassLoader> wr : list) {
          if (wr != null && wr.get() == cl) {
            list.remove(wr);
            break;
          }
        }
      }
      if (list.isEmpty()) {
        map.remove(className);
      }
    }
  }

  @Override
  public Map<String, List<WeakReference<ClassLoader>>> getLoadedClasses() {
    return unModifableLoadedClassesMap;
  }

  @Override
  public Map<String, List<WeakReference<ClassLoader>>> getTransformedClasses() {
    return unModifableTransformedClassesMap;
  }
}
