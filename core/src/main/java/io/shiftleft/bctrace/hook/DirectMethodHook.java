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
package io.shiftleft.bctrace.hook;

import io.shiftleft.bctrace.filter.MethodFilter.DirectMethodFilter;
import io.shiftleft.bctrace.hook.util.DirectListenerValidator;
import io.shiftleft.bctrace.runtime.listener.direct.DirectMethodListener;
import io.shiftleft.bctrace.runtime.listener.direct.DirectMethodReturnListener;
import io.shiftleft.bctrace.runtime.listener.direct.DirectMethodStartListener;
import io.shiftleft.bctrace.runtime.listener.direct.DirectMethodThrowableListener;

public class DirectMethodHook extends Hook<DirectMethodFilter, DirectMethodListener> {

  public DirectMethodHook(DirectMethodFilter filter, DirectMethodListener listener) {
    super(filter, listener);
    DirectListenerValidator.checkListenerMethod(filter.getMethodDescriptor(), listener);
  }

  public static void main(String[] args) {
    new DirectMethodHook(new DirectMethodFilter("AA", "mm", "(Ljava/lang/String;D)I"),
        new DirectMethodReturnListener(){

          @ListenerMethod
          public int listen(Class clazz, Object instance, String str, double i, int ret, int y) {
            return 1;
          }
        });
  }
}
