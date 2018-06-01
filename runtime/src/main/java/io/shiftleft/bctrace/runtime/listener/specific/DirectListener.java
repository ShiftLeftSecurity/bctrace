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
package io.shiftleft.bctrace.runtime.listener.specific;

import io.shiftleft.bctrace.runtime.listener.Listener;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * DirectListener instances are notified directly from the instrumented method without creating
 * intermediate object instance. They are used for high performance notification.
 *
 * DirectListener does not define a fixed interface. They are used in TargetedHooks, and they must
 * define a single method (with name defined in the ListenerType enum) whose signature matches the
 * signature of the TargetedFilter of the hook in the following way:
 *
 * Suppose the instrumented method defined by the filter being foo(Integer arg1, String arg2), then
 * a valid direct listener method for receiving start events would be:
 *
 * @author Ignacio del Valle Alles idelvall@shiftleft.io
 * @ListenerMethod public void onStart(Class clazz, Object instance, Integer arg1, String arg2)
 */
public interface DirectListener extends Listener {

  /**
   * Defines the valid names for the listener method, and its fixed first arguments
   */
  public static enum ListenerType {

    // Method id, Instrumented class, instrumented instance
    onStart(int.class, Class.class, Object.class),
    // Method id, caller class, caller instance, callee class, callee instance
    onBeforeCall(int.class, Class.class, Object.class, Class.class, Object.class);

    private Class[] fixedArgs;

    ListenerType(Class... fixedArgs) {
      this.fixedArgs = fixedArgs;
    }

    public Class[] getFixedArgs() {
      return fixedArgs;
    }
  }

  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.METHOD)
  public static @interface ListenerMethod {

    public ListenerType type();
  }
}
