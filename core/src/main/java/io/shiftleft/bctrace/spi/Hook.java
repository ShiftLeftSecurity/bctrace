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

import io.shiftleft.bctrace.runtime.listener.Listener;
import io.shiftleft.bctrace.spi.Filter;
import io.shiftleft.bctrace.spi.Instrumentation;

/**
 * An <b>instrumentation hook</b> determines what methods to instrument and what
 * actions to perform at runtime under the events triggered by the instrumented
 * methods.
 *
 * @author Ignacio del Valle Alles idelvall@shiftleft.io
 */
public abstract class Hook {

  protected Instrumentation instrumentation;

  private final String jvmPackage;

  public Hook() {
    this.jvmPackage = getClass().getPackage().getName().replace('.', '/') + "/";
  }

  /**
   * Initializes the plugin. Called once at startup before initial
   * instrumentation is performed.
   *
   * @param instrumentation Intrumentation callback, allowing triggering
   * retransformations
   */
  public final void init(Instrumentation instrumentation) {
    this.instrumentation = instrumentation;
    doInit();
  }

  /**
   * Allows subclasses to implement initialization logic.
   */
  public void doInit() {
  }

  public final Instrumentation getInstrumentation() {
    return instrumentation;
  }

  public final String getJvmPackage() {
    return jvmPackage;
  }

  /**
   * Returns the filter, deciding what methods to instrument.
   *
   * @return
   */
  public abstract Filter getFilter();

  /**
   * Returns the listener invoked by the instrumented method hooks.
   *
   * @return
   */
  public abstract Listener getListener();

  /**
   * Communicates an error to the hook implementation
   *
   * @param th
   */
  public void onError(Throwable th) {

  }
}
