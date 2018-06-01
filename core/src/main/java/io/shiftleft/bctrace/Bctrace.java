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
import io.shiftleft.bctrace.impl.InstrumentationImpl;
import io.shiftleft.bctrace.runtime.Callback;
import io.shiftleft.bctrace.runtime.listener.Listener;
import io.shiftleft.bctrace.spi.Agent;
import io.shiftleft.bctrace.spi.AgentLoggerFactory;
import io.shiftleft.bctrace.spi.Hook;
import io.shiftleft.bctrace.spi.Instrumentation;
import io.shiftleft.bctrace.spi.SystemProperty;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.security.ProtectionDomain;
import java.util.Arrays;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

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
        System.out.println(Arrays.toString(Callback.class.getDeclaredMethods()));
        Transformer transformer = new Transformer(this.instrumentation, NATIVE_WRAPPER_PREFIX,
            this);
        instrumentation.getJavaInstrumentation().addTransformer(transformer, true);
        instrumentation.getJavaInstrumentation()
            .setNativeMethodPrefix(transformer, NATIVE_WRAPPER_PREFIX);
      }
      agent.afterRegistration();
    }
  }

  private static Logger createLogger() {
    Logger logger = AgentLoggerFactory.getInstance().getLogger();
    String logLevel = System.getProperty(SystemProperty.LOG_LEVEL);
    if (logLevel != null) {
      Level level = Level.parse(logLevel);
      logger.setLevel(level);
      Handler[] handlers = logger.getHandlers();
      if (handlers != null) {
        for (Handler handler : handlers) {
          handler.setLevel(level);
        }
      }
    }
    return logger;
  }

  public Instrumentation getInstrumentation() {
    return this.instrumentation;
  }

  public Hook[] getHooks() {
    return this.hooks;
  }

  public static URL getCodeSource(String className, ProtectionDomain protectionDomain,
      ClassLoader cl) {
    if (protectionDomain != null && protectionDomain.getCodeSource() != null) {
      return protectionDomain.getCodeSource().getLocation();
    }
    if (cl == null) {
      return null;
    }
    String resourceName = className.replace('.', '/') + ".class";
    URL url = cl.getResource(className.replace('.', '/') + ".class");
    return getCodeSource(resourceName, url);
  }

  public static URL getCodeSource(Class clazz) {
    return getCodeSource(clazz.getName(), clazz.getProtectionDomain(), clazz.getClassLoader());
  }

  public static URL getCodeSource(String resourceName, URL resourceUrl) {
    if (resourceUrl == null) {
      return null;
    }
    URLConnection urlConnection;
    try {
      urlConnection = resourceUrl.openConnection();
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
    if (urlConnection instanceof JarURLConnection) {
      JarURLConnection jarURLConnection = (JarURLConnection) urlConnection;
      return jarURLConnection.getJarFileURL();
    }
    String str = resourceUrl.toString().replace(resourceName, "");
    if (str.endsWith("!/")) {
      str = str.substring(0, str.length() - 2);
    } else if (str.endsWith("!")) {
      str = str.substring(0, str.length() - 1);
    }
    try {
      return new URL(str);
    } catch (MalformedURLException ex) {
      throw new AssertionError();
    }
  }


  public static Logger getAgentLogger() {
    return LOGGER;
  }
}
