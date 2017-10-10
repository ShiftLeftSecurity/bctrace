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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Ignacio del Valle Alles idelvall@shiftleft.io
 */
public abstract class AgentLoggerFactory {

  private static AgentLoggerFactory instance;

  static {
    ServiceLoader<AgentLoggerFactory> sl = ServiceLoader.load(AgentLoggerFactory.class, AgentLoggerFactory.class.getClassLoader());
    Iterator<AgentLoggerFactory> it = sl.iterator();
    List<AgentLoggerFactory> instances = new ArrayList<AgentLoggerFactory>();
    while (it.hasNext()) {
      instances.add(it.next());
    }
    if (instances.isEmpty()) {
      instance = new DefaultLoggerFactory();
    } else if (instances.size() > 1) {
      throw new Error(
              "Multiple '" + AgentLoggerFactory.class.getSimpleName() + "' service providers found: "
              + instances);
    } else {
      instance = instances.get(0);
    }
  }

  public static AgentLoggerFactory getInstance() {
    return instance;
  }

  public abstract Logger getLogger();

  private static class DefaultLoggerFactory extends AgentLoggerFactory {

    private static final Logger LOGGER = createLogger();

    private static Logger createLogger() {
      Logger logger = Logger.getAnonymousLogger();
      logger.setLevel(Level.ALL);
      logger.addHandler(new ConsoleHandler());
      logger.setUseParentHandlers(false);
      logger.info("Starting bctrace agent");
      return logger;
    }

    @Override
    public Logger getLogger() {
      return LOGGER;
    }
  }
}
