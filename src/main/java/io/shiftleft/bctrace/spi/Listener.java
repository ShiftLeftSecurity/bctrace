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

import io.shiftleft.bctrace.runtime.FrameData;

/**
 * Listener instances define the actions to be performed when a instrumented
 * method is invoked.
 *
 * @author Ignacio del Valle Alles idelvall@shiftleft.io
 */
public interface Listener {

  /**
   * Invoked by instrumented methods before any of its original instructions (if
   * multiple plugins are registered, listener notification is performed
   * according to their respective plugin registration order).
   *
   * @param fd Current stack frame data
   * @return
   */
  public void onStart(FrameData fd);

  /**
   * Invoked by instrumented methods just before return (if multiple plugins are
   * registered, listener notification is performed according to their
   * respective plugin <b>reverse</b> registration order).
   *
   * @param ret Object being returned by the method. Wrapper type if the
   * original return type is primitive. <code>null</code> if the method return
   * type is <code>void</code>
   * @param fd Current stack frame data
   */
  public void onFinishedReturn(Object ret, FrameData fd);

  /**
   * Invoked by instrumented methods just before rising a throwable to the
   * caller (if multiple plugins are registered, listener notification is
   * performed according to their respective plugin <b>reverse</b>
   * registration order).
   *
   * @param th thowable to be raised
   * @param fd Current stack frame data
   */
  public void onFinishedThrowable(Throwable th, FrameData fd);

  /**
   * Invoked by instrumented methods just before the actual method throws a
   * throwable.
   *
   * @param th thowable to be thrown
   * @param fd Current stack frame data
   */
  public void onBeforeThrown(Throwable th, FrameData fd);

}
