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
package io.shiftleft.bctrace.debug;

import io.shiftleft.bctrace.spi.SystemProperties;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 * @author Ignacio del Valle Alles idelvall@shiftleft.io
 */
public class DebugInfo {

  private static final DebugInfo INSTANCE;

  static {
    String debugServer = System.getProperty(SystemProperties.DEBUG_SERVER);
    if (debugServer != null) {
      try {
        String[] tokens = debugServer.split(":");
        if (tokens.length != 2) {
          throw new Error("Invalid system property " + SystemProperties.DEBUG_SERVER + ". Value has to be in the form 'hostname:port'");
        }
        new HttpServer(tokens[0], Integer.valueOf(tokens[1]));
        INSTANCE = new DebugInfo();
      } catch (IOException ex) {
        throw new RuntimeException(ex);
      }
    } else {
      INSTANCE = null;
    }
  }

  private final Map<Integer, AtomicInteger> instrumentedMethods = Collections.synchronizedMap(new HashMap<Integer, AtomicInteger>());

  public static DebugInfo getInstance() {
    return INSTANCE;
  }

  public void setInstrumented(Integer methodId, boolean instrumented) {
    if (instrumented) {
      instrumentedMethods.put(methodId, new AtomicInteger());
    } else {
      instrumentedMethods.remove(methodId);
    }
  }

  public void increaseCallCounter(Integer methodId) {
    instrumentedMethods.get(methodId).incrementAndGet();
  }

  public Integer getCallCounter(Integer methodId) {
    AtomicInteger counter = instrumentedMethods.get(methodId);
    if (counter == null) {
      return null;
    }
    return counter.get();
  }
}
