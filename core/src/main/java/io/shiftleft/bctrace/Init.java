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
package io.shiftleft.bctrace;

import io.shiftleft.bctrace.asm.DirectListenerTransformer;
import io.shiftleft.bctrace.asm.util.Unsafe;
import io.shiftleft.bctrace.debug.DebugHttpServer;
import io.shiftleft.bctrace.runtime.CallbackEnabler;
import io.shiftleft.bctrace.runtime.listener.direct.DirectListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Scanner;
import java.util.Set;
import sun.misc.IOUtils;

/**
 * Framework entry point.
 *
 * @author Ignacio del Valle Alles idelvall@shiftleft.io
 */
public class Init {

  private static final String OSGI_PACKAGE_DELEGATION_PROP = "org.osgi.framework.bootdelegation";
  private static final String OSGI_PACKAGE_DELEGATION_VALUE = "io.shiftleft.*";
  private static final String DESCRIPTOR_NAME = ".bctrace";

  public static void premain(final String arg, Instrumentation inst) throws Exception {
    bootstrap(arg, inst);
  }

  public static void agentmain(String arg, Instrumentation inst) throws Exception {
    bootstrap(arg, inst);
  }

  private static void bootstrap(String agentArgs, Instrumentation inst) throws Exception {
    CallbackEnabler.disableThreadNotification();
    wrapSystemProperties();
    String factoryImpClass = readAgentFactoryImpClass();
    if (factoryImpClass == null) {
      throw new Error("No agent factory found in classpath resource " + DESCRIPTOR_NAME);
    }
    InstrumentationImpl instrumentation = new InstrumentationImpl(inst);
    DirectListenerTransformer directListenerTransformer = new DirectListenerTransformer(instrumentation);
    inst.addTransformer(directListenerTransformer, false);
    AgentFactory agentFactory = (AgentFactory) Class.forName(factoryImpClass).newInstance();
    Bctrace bctrace = new Bctrace(instrumentation, agentFactory.createAgent());
    bctrace.init();
    DebugHttpServer.init();
    CallbackEnabler.enableThreadNotification();
  }

  private static void wrapSystemProperties() {
    Properties initialProperties = System.getProperties();
    System.setProperties(new Properties() {
      @Override
      public synchronized Object put(Object key, Object value) {
        if (key.equals(OSGI_PACKAGE_DELEGATION_PROP)) {
          value = value + "," + OSGI_PACKAGE_DELEGATION_VALUE;
        }
        return super.put(key, value);
      }
    });
    Set<Entry<Object, Object>> entries = initialProperties.entrySet();
    for (Entry entry : entries) {
      System.setProperty((String) (entry.getKey()), (String) (entry.getValue()));
    }
  }

  private static String readAgentFactoryImpClass() throws IOException {
    ClassLoader cl = Init.class.getClassLoader();
    if (cl == null) {
      cl = ClassLoader.getSystemClassLoader().getParent();
    }
    Scanner scanner = new Scanner(cl.getResourceAsStream(DESCRIPTOR_NAME));
    if (scanner.hasNextLine()) {
      return scanner.nextLine().trim();
    }
    return null;
  }
}
