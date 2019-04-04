package ${package}.${artifactIdUnhyphenated}.log;

import io.shiftleft.bctrace.logging.Level;
import io.shiftleft.bctrace.logging.Logger;
import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;

public class AgentLoggerFactory extends io.shiftleft.bctrace.logging.AgentLoggerFactory {

  private final Logger logger;

  public AgentLoggerFactory() {
    this.logger = new Logger() {
      @Override
      protected void doLog(Level level, String s,
          Throwable throwable) {
        StringBuilder sb = new StringBuilder(level.name());
        sb.append(" ");
        sb.append(System.currentTimeMillis());
        if (s != null) {
          sb.append(" ").append(s);
        }
        if (throwable != null) {
          sb.append(" ").append(getStackTrace(throwable));
        }
        System.err.println(sb);
      }
    };
    this.logger.setLevel(Level.INFO);
  }

  @Override
  public Logger getLogger() {
    return this.logger;
  }

  private static String getStackTrace(Throwable th) {
    if (th == null) {
      return null;
    }
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    PrintWriter pw = new PrintWriter(baos);
    th.printStackTrace(pw);
    pw.close();
    return baos.toString();
  }
}