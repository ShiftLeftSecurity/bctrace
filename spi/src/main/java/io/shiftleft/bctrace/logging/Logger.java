package io.shiftleft.bctrace.logging;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public abstract class Logger {

  private final Set<Logger> delegators = Collections.synchronizedSet(new HashSet());

  protected Level level = Level.QUIET;

  public final void setLevel(Level level) {
    this.level = level;
  }

  public final Level getLevel() {
    return level;
  }

  public final boolean isLoggable(Level level) {
    return this.level != Level.QUIET && this.level.value <= level.value;
  }

  public void addDelegator(Logger logger) {
    this.delegators.add(logger);
  }

  public void removeDelegator(Logger logger) {
    this.delegators.remove(logger);
  }

  public final void log(Level level, String message) {
    log(level, message, null);
  }

  public final void log(LogRecord lr) {
    log(lr.getLevel(), lr.getMessage(), lr.getThrowable());
  }

  public final void log(Level level, String message, Throwable th) {
    if (isLoggable(level)) {
      doLog(level, message, th);
      for (Logger delegator : delegators) {
        delegator.log(level, message, th);
      }
    }
  }

  protected abstract void doLog(Level level, String message, Throwable th);
}
