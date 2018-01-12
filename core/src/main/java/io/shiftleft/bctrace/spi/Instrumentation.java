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
package io.shiftleft.bctrace.spi;

import java.lang.instrument.UnmodifiableClassException;
import java.util.List;

/**
 * @author Ignacio del Valle Alles idelvall@shiftleft.io
 */
public interface Instrumentation {

  /**
   * Whether or not this JVM supports class retransformation.
   *
   * @see <a href="https://docs.oracle.com/javase/6/docs/api/java/lang/instrument/Instrumentation.html#isRetransformClassesSupported()">Instrumentation.isRetransformClassesSupported()</a>
   */
  boolean isRetransformClassesSupported();

  /**
   * Whether or not this class can be retransformed.
   */
  boolean isModifiableClass(Class<?> clazz);

  /**
   * Whether or not this class can be retransformed.
   *
   * @param jvmClassName class name according to the JVM spec (packages separated by '/')
   */
  boolean isModifiableClass(String jvmClassName);

  /**
   * Retransforms the classes.
   *
   * @see <a href="https://docs.oracle.com/javase/6/docs/api/java/lang/instrument/Instrumentation.html#retransformClasses(java.lang.Class...)">Instrumentation.retransformClasses(java.lang.Class...)</a>
   */
  void retransformClasses(Class<?>... classes) throws UnmodifiableClassException;

  /**
   *
   * @param className
   * @return
   */
  public boolean isLoadedByAnyClassLoader(String className);

  /**
   * Returns a class with no side effects (if not loaded it remains not loaded), without using the
   * expensive instrumentation.getAllLoadedClasses()
   */
  public Class getClassIfLoadedByClassLoader(String className, ClassLoader cl);

  /**
   *
   * @param className
   * @param cl
   * @return
   */
  public boolean isLoadedBy(String className, ClassLoader cl);

  /**
   *
   * @param className
   * @return
   */
  public List<ClassLoader> getClassLoadersLoading(String className);

  /**
   *
   * @return
   */
  public Class[] getAllLoadedClasses();
}

