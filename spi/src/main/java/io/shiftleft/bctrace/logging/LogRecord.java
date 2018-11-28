package io.shiftleft.bctrace.logging;

public class LogRecord {

  private final Level level;
  private final String message;
  private final Throwable throwable;

  public LogRecord(Level level, String message) {
    this(level, message, null);
  }

  public LogRecord(Level level, String message, Throwable throwable) {
    this.level = level;
    this.message = message;
    this.throwable = throwable;
  }

  public Level getLevel() {
    return level;
  }

  public String getMessage() {
    return message;
  }

  public Throwable getThrowable() {
    return throwable;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    LogRecord logRecord = (LogRecord) o;

    if (level != logRecord.level) {
      return false;
    }
    if (message != null ? !message.equals(logRecord.message) : logRecord.message != null) {
      return false;
    }
    return throwable != null ? throwable.equals(logRecord.throwable) : logRecord.throwable == null;
  }

  @Override
  public int hashCode() {
    int result = level != null ? level.hashCode() : 0;
    result = 31 * result + (message != null ? message.hashCode() : 0);
    result = 31 * result + (throwable != null ? throwable.hashCode() : 0);
    return result;
  }
}
