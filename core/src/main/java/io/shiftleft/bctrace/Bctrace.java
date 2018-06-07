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
package io.shiftleft.bctrace;

import io.shiftleft.bctrace.asm.CallbackTransformer;
import io.shiftleft.bctrace.asm.Transformer;
import io.shiftleft.bctrace.debug.CallCounterHook;
import io.shiftleft.bctrace.debug.DebugInfo;
import io.shiftleft.bctrace.hook.Hook;
import io.shiftleft.bctrace.logging.AgentLoggerFactory;
import io.shiftleft.bctrace.logging.Level;
import io.shiftleft.bctrace.logging.Logger;
import io.shiftleft.bctrace.runtime.Callback;
import io.shiftleft.bctrace.runtime.listener.Listener;
import java.net.URL;

/**
 * Framework entry point.
 *
 * @author Ignacio del Valle Alles idelvall@shiftleft.io
 */
public final class Bctrace {

  private static final Logger LOGGER = createLogger();
  private static final String NATIVE_WRAPPER_PREFIX =
      "$$$Bctrace_Wrapper_" + System.currentTimeMillis() + "$$$_";

  private final InstrumentationImpl instrumentation;
  private final Hook[] hooks;
  private final Agent agent;

  Bctrace(java.lang.instrument.Instrumentation javaInstrumentation, Agent agent) {
    this.agent = agent;
    this.instrumentation = new InstrumentationImpl(javaInstrumentation);
    if (DebugInfo.isEnabled()) {
      this.hooks = new Hook[agent.getHooks().length + 1];
      System.arraycopy(agent.getHooks(), 0, this.hooks, 0, agent.getHooks().length);
      this.hooks[agent.getHooks().length] = new CallCounterHook();
    } else {
      this.hooks = agent.getHooks();
    }
  }

  public void init() {
    if (agent != null) {
      agent.init(this);
      Listener[] listeners = new Listener[hooks.length];
      for (int i = 0; i < hooks.length; i++) {
        listeners[i] = this.hooks[i].getListener();
      }
      if (instrumentation != null && instrumentation.getJavaInstrumentation() != null) {
        instrumentation.getJavaInstrumentation()
            .addTransformer(new CallbackTransformer(hooks), false);
        Callback.listeners = listeners;
        Transformer transformer = new Transformer(this.instrumentation, NATIVE_WRAPPER_PREFIX,
            this);
        instrumentation.getJavaInstrumentation().addTransformer(transformer, true);
        instrumentation.getJavaInstrumentation()
            .setNativeMethodPrefix(transformer, NATIVE_WRAPPER_PREFIX);
      }
      agent.afterRegistration();
    }
  }

  public void disableThreadNotification() {
    Callback.disableThreadNotification();
  }

  public void enableThreadNotification() {
    Callback.enableThreadNotification();
  }

  public void isThreadNotificationEnabled() {
    Callback.isThreadNotificationEnabled();
  }

  public static URL getURL(Class clazz) {
    String resourceName = clazz.getName().replace('.', '/') + ".class";
    ClassLoader cl = clazz.getClassLoader();
    if (cl == null) {
      return ClassLoader.getSystemResource(resourceName);
    } else {
      return cl.getResource(resourceName);
    }
  }

  private static Logger createLogger() {
    Logger logger = AgentLoggerFactory.getInstance().getLogger();
    String logLevel = System.getProperty(SystemProperty.LOG_LEVEL);
    if (logLevel != null) {
      Level level = Level.valueOf(logLevel);
      logger.setLevel(level);
    }
    return logger;
  }

  public Instrumentation getInstrumentation() {
    return this.instrumentation;
  }

  public Hook[] getHooks() {
    return this.hooks;
  }

  public static Logger getAgentLogger() {
    return LOGGER;
  }
}
