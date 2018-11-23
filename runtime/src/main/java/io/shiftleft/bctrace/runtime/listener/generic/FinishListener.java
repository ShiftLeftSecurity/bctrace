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
package io.shiftleft.bctrace.runtime.listener.generic;

import java.lang.reflect.Method;

/**
 * @author Ignacio del Valle Alles idelvall@shiftleft.io
 */
public abstract class FinishListener extends GenericListener {

  @Override
  protected final Method getListenerSuperMethod() {
    return getMethodByName(FinishListener.class, "onFinish");
  }

  /**
   * Invoked by instrumented methods just before returning or raising a throwable (if multiple
   * plugins are registered, listener notification is performed according to their respective
   * plugin
   * <b>reverse</b> registration order).
   *
   * @param methodId method id (as defined by MethodRegistry)
   * @param clazz class defining the method.
   * @param instance instance where the method is invoked. Null if the method is static
   * @param args arguments passed to the method.
   * @param ret Object being returned by the method. Wrapper type if the original return type is
   * primitive. <code>null</code> if the method return type is <code>void</code> or the method
   * raises a throwable
   * @param th The throwable raised by the target method.  <code>null</code> if the method returns
   * normally
   * @return Object to be returned by the instrumented method  (ignored if target method return type
   * is void) or throwable to be raised
   */
  public abstract Object onFinish(int methodId, Class clazz, Object instance, Object[] args,
      Object ret, Throwable th);

}
