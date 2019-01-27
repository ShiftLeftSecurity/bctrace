package ${package}.${artifactIdUnhyphenated}.log;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;

public class AgentLoggerFactory extends io.shiftleft.bctrace.logging.AgentLoggerFactory {

  @Override
  public io.shiftleft.bctrace.logging.Logger getLogger() {
    io.shiftleft.bctrace.logging.Logger logger = new io.shiftleft.bctrace.logging.Logger() {
      @Override
      protected void doLog(io.shiftleft.bctrace.logging.Level level, String s,
          Throwable throwable) {
        StringBuilder sb = new StringBuilder(level.name());
        sb.append(" ");
        sb.append(System.currentTimeMillis());
        if(s!=null){
          sb.append(" ").append(s);
        }
        if(throwable!=null){
          sb.append(" ").append(getStrackTrace(throwable));
        }
      }
    };
    return logger;
  }

  private static String getStrackTrace(Throwable th) {
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
