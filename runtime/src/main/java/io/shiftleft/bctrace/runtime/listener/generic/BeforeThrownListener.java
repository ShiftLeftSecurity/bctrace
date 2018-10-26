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
public abstract class BeforeThrownListener extends GenericListener {

  private static final Method METHOD = BeforeThrownListener.class.getDeclaredMethods()[0];

  @Override
  protected final Method getListenerSuperMethod() {
    return METHOD;
  }

  public final boolean requiresThrowable() {
    return !getDisabledArguments()[4];
  }

  /**
   * Invoked by instrumented methods just before the actual method throws a throwable.
   *
   * @param methodId method id (as defined by MethodRegistry)
   * @param clazz class defining the method.
   * @param instance instance where the method is invoked. Null if the method is static
   * @param args arguments passed to the method.
   * @param th throwable to be thrown
   */
  public abstract void onBeforeThrown(int methodId, Class clazz, Object instance, Object[] args, Throwable th);

}
