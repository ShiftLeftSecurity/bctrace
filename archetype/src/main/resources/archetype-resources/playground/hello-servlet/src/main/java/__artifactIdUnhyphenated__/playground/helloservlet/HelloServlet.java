package ${package}.${artifactIdUnhyphenated}.playground.helloservlet;

import java.io.IOException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class HelloServlet extends HttpServlet {

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    ServletOutputStream out = resp.getOutputStream();
    resp.setContentType("text/plain");
    out.write(getGreetings(req).getBytes());
    out.flush();
    out.close();
  }

  private static String getGreetings(HttpServletRequest req) {
    StringBuilder sb = new StringBuilder();
    String[] names = req.getParameterValues("name");
    if (names != null) {
      for (int i = 0; i < names.length; i++) {
        sb.append((getGreeting(names[i]) + "\n"));
      }
    }
    return sb.toString();
  }

  private static String getGreeting(String name) {
    if (name != null) {
      return "Hello " + name + "!";
    } else {
      return "Hello word!";
    }
  }
}
