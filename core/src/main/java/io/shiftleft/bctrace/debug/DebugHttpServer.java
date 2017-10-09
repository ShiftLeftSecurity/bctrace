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

import io.shiftleft.bctrace.runtime.DebugInfo;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import io.shiftleft.bctrace.Bctrace;
import io.shiftleft.bctrace.spi.MethodInfo;
import io.shiftleft.bctrace.spi.MethodRegistry;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import io.shiftleft.bctrace.spi.SystemProperty;

/**
 *
 * @author Ignacio del Valle Alles idelvall@shiftleft.io
 */
public class DebugHttpServer {

  static {
    DebugInfo.enabled = System.getProperty(SystemProperty.DEBUG_SERVER) != null;
  }

  public static void init() {
    String debugServer = System.getProperty(SystemProperty.DEBUG_SERVER);
    if (debugServer != null) {
      try {
        Bctrace.getInstance().getAgentLogger().severe("Starting debug server at " + debugServer);
        String[] tokens = debugServer.split(":");
        if (tokens.length != 2) {
          throw new Error("Invalid system property " + SystemProperty.DEBUG_SERVER + ". Value has to be in the form 'hostname:port'");
        }
        new DebugHttpServer(tokens[0], Integer.valueOf(tokens[1]));
      } catch (IOException ex) {
        throw new RuntimeException(ex);
      }
    }
  }

  private DebugHttpServer(String hostName, int port) throws IOException {
    com.sun.net.httpserver.HttpServer server = com.sun.net.httpserver.HttpServer.create(new InetSocketAddress(hostName, port), 0);
    server.createContext("/", new RootHandler());
    server.createContext("/methods", new MethodRegistryHandler());
    server.createContext("/methods/instrumented", new InstrumentedMethodsHandler());
    server.createContext("/classes/instrumented", new InstrumentedClassesHandler());
    server.createContext("/classes/requested", new RequestedToInstrumentClassesHandler());
    server.start();
  }

  private static class RequestedToInstrumentClassesHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange t) throws IOException {
      StringBuilder sb = new StringBuilder();
      sb.append("[");
      boolean first = true;
      DebugInfo.ClassInfo[] instrumented = DebugInfo.getInstance().getRequestedToInstrumentation();
      for (DebugInfo.ClassInfo ci : instrumented) {
        if (!first) {
          sb.append(",");
        }
        first = false;
        sb.append("\n");
        sb.append("{");
        sb.append("\"className\":").append("\"").append(ci.getClassName()).append("\"").append(",");
        sb.append("\"classLoader\":").append("\"").append(ci.getClassLoader()).append("\"").append(",");
        sb.append("}");
      }
      sb.append("\n");
      sb.append("]");
      MethodRegistry.getInstance().size();
      byte[] bytes = sb.toString().getBytes();
      t.getResponseHeaders().add("Content-Type", "application/json");
      t.sendResponseHeaders(200, bytes.length);
      OutputStream os = t.getResponseBody();
      os.write(bytes);
      os.close();
    }
  }

  private static class InstrumentedClassesHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange t) throws IOException {
      StringBuilder sb = new StringBuilder();
      sb.append("[");
      boolean first = true;
      DebugInfo.ClassInfo[] instrumented = DebugInfo.getInstance().getInstrumented();
      for (DebugInfo.ClassInfo ci : instrumented) {
        if (!first) {
          sb.append(",");
        }
        first = false;
        sb.append("\n");
        sb.append("{");
        sb.append("\"className\":").append("\"").append(ci.getClassName()).append("\"").append(",");
        sb.append("\"classLoader\":").append("\"").append(ci.getClassLoader()).append("\"");
        sb.append("}");
      }
      sb.append("\n");
      sb.append("]");
      MethodRegistry.getInstance().size();
      byte[] bytes = sb.toString().getBytes();
      t.getResponseHeaders().add("Content-Type", "application/json");
      t.sendResponseHeaders(200, bytes.length);
      OutputStream os = t.getResponseBody();
      os.write(bytes);
      os.close();
    }
  }

  private static class InstrumentedMethodsHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange t) throws IOException {
      StringBuilder sb = new StringBuilder();
      sb.append("[");
      boolean first = true;
      for (int i = 0; i < MethodRegistry.getInstance().size(); i++) {
        Integer callCounter = DebugInfo.getInstance().getCallCounter(i);
        MethodInfo mi = MethodRegistry.getInstance().getMethod(i);
        if (callCounter != null) {
          if (!first) {
            sb.append(",");
          }
          first = false;
          sb.append("\n");
          sb.append("{");
          sb.append("\"id\":").append(i).append(",");
          sb.append("\"className\":").append("\"").append(mi.getBinaryClassName()).append("\"").append(",");
          sb.append("\"method\":").append("\"").append(mi.getMethodName()).append(mi.getMethodDescriptor()).append("\"");
          sb.append("\"callCounter\":").append(callCounter);
          sb.append("}");
        }
      }
      sb.append("\n");
      sb.append("]");
      MethodRegistry.getInstance().size();
      byte[] bytes = sb.toString().getBytes();
      t.getResponseHeaders().add("Content-Type", "application/json");
      t.sendResponseHeaders(200, bytes.length);
      OutputStream os = t.getResponseBody();
      os.write(bytes);
      os.close();
    }
  }

  private static class MethodRegistryHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange t) throws IOException {
      StringBuilder sb = new StringBuilder();
      sb.append("[");
      for (int i = 0; i < MethodRegistry.getInstance().size(); i++) {
        MethodInfo mi = MethodRegistry.getInstance().getMethod(i);
        if (i > 0) {
          sb.append(",");
        }
        sb.append("\n");
        sb.append("{");
        sb.append("\"id\":").append(i).append(",");
        sb.append("\"className\":").append("\"").append(mi.getBinaryClassName()).append("\"").append(",");
        sb.append("\"method\":").append("\"").append(mi.getMethodName()).append(mi.getMethodDescriptor()).append("\"");
        sb.append("}");
      }
      sb.append("\n");
      sb.append("]");
      MethodRegistry.getInstance().size();
      byte[] bytes = sb.toString().getBytes();
      t.getResponseHeaders().add("Content-Type", "application/json");
      t.sendResponseHeaders(200, bytes.length);
      OutputStream os = t.getResponseBody();
      os.write(bytes);
      os.close();
    }
  }

  private static class RootHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange t) throws IOException {
      StringBuilder sb = new StringBuilder();
      sb.append("<h1>Bctrace stats API</h1>");
      sb.append("<ul>");
      sb.append("<li><a href=\"/methods\">All methods</a></li>");
      sb.append("<li><a href=\"/methods/instrumented\">Instrumented methods</a></li>");
      sb.append("<li><a href=\"/classes/instrumented\">Instrumented classes</a></li>");
      sb.append("<li><a href=\"/classes/requested\">Classes requested to instrument (via API)</a></li>");
      sb.append("</ul>");
      byte[] bytes = sb.toString().getBytes();
      t.sendResponseHeaders(200, bytes.length);
      OutputStream os = t.getResponseBody();
      os.write(bytes);
      os.close();
    }
  }
}
