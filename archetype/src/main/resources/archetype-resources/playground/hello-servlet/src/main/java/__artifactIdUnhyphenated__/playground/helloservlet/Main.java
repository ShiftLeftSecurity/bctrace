package ${package}.${artifactIdUnhyphenated}.playground.helloservlet;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

public class Main {

  public static void main(String[] args) throws Exception {
    Server server = new Server(8080);

    ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
    context.setContextPath("/");
    server.setHandler(context);
    context.addServlet(new ServletHolder(new HelloServlet()), "/hello");
    server.start();
    System.err.println("http://localhost:8080/hello?name=world&name=bctrace");
    server.join();
  }
}

