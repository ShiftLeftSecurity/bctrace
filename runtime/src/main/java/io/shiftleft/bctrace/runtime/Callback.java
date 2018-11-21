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
package io.shiftleft.bctrace.runtime;

import io.shiftleft.bctrace.runtime.listener.generic.BeforeThrownListener;
import io.shiftleft.bctrace.runtime.listener.generic.FinishListener;
import io.shiftleft.bctrace.runtime.listener.generic.StartListener;

/**
 * @author Ignacio del Valle Alles idelvall@shiftleft.io
 */
public final class Callback {

  public static Object[] listeners;
  public static ErrorListener errorListener;

  // Avoid notifications caused by listener methods code
  private static final ThreadLocal<Boolean> NOTIFYING_FLAG = new ThreadLocal<Boolean>();

  @SuppressWarnings("BoxedValueEquality")
  public static void onStart(Object[] args, int methodId, Class clazz, Object instance, int i) {
    if (!CallbackEnabler.isThreadNotificationEnabled()) {
      return;
    }
    if (Boolean.TRUE == NOTIFYING_FLAG.get()) {
      return;
    }
    try {
      NOTIFYING_FLAG.set(Boolean.TRUE);
      ((StartListener) listeners[i]).onStart(methodId, clazz, instance, args);
    } catch (Throwable th) {
      handleThrowable(th);
    } finally {
      NOTIFYING_FLAG.set(Boolean.FALSE);
    }
  }

  @SuppressWarnings("BoxedValueEquality")
  public static Object onFinish(Object ret, Throwable th, int methodId, Class clazz,
      Object instance,
      int i, Object[] args) {
    if (!CallbackEnabler.isThreadNotificationEnabled()) {
      return ret;
    }
    if (Boolean.TRUE == NOTIFYING_FLAG.get()) {
      return ret;
    }
    try {
      NOTIFYING_FLAG.set(Boolean.TRUE);
      return ((FinishListener) listeners[i])
          .onFinish(methodId, clazz, instance, args, ret, th);
    } catch (Throwable thr) {
      handleThrowable(thr);
      return ret;
    } finally {
      NOTIFYING_FLAG.set(Boolean.FALSE);
    }
  }

  @SuppressWarnings("BoxedValueEquality")
  public static void onBeforeThrow(Throwable throwable, int methodId, Class clazz, Object instance,
      int i, Object[] args) {
    if (!CallbackEnabler.isThreadNotificationEnabled()) {
      return;
    }
    if (Boolean.TRUE == NOTIFYING_FLAG.get()) {
      return;
    }
    try {
      NOTIFYING_FLAG.set(Boolean.TRUE);
      ((BeforeThrownListener) listeners[i])
          .onBeforeThrow(methodId, clazz, instance, args, throwable);
    } catch (Throwable th) {
      handleThrowable(th);
    } finally {
      NOTIFYING_FLAG.set(Boolean.FALSE);
    }
  }

  private static void handleThrowable(Throwable th) {
    if (th instanceof BctraceRuntimeException) {
      throw ((BctraceRuntimeException) th).getWrappedException();
    } else {
      if (errorListener != null) {
        errorListener.onError(th);
      } else {
        th.printStackTrace();
      }
    }
  }

  public static interface ErrorListener {

    public void onError(Throwable th);
  }
}
