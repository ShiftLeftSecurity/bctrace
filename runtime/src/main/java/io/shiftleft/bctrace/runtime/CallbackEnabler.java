package io.shiftleft.bctrace.runtime;

import java.util.concurrent.atomic.AtomicInteger;

public class CallbackEnabler {

  public static final ThreadLocal<AtomicInteger> NOTIFY_DISABLED_FLAG = new ThreadLocal<AtomicInteger>() {
    @Override
    protected AtomicInteger initialValue() {
      return new AtomicInteger(0);
    }
  };

  @SuppressWarnings("BoxedValueEquality")
  public static boolean isThreadNotificationEnabled() {
    return NOTIFY_DISABLED_FLAG.get().intValue() == 0;
  }

  @SuppressWarnings("BoxedValueEquality")
  public static void enableThreadNotification() {
    NOTIFY_DISABLED_FLAG.get().decrementAndGet();
  }

  @SuppressWarnings("BoxedValueEquality")
  public static void disableThreadNotification() {
    NOTIFY_DISABLED_FLAG.get().incrementAndGet();
  }
}
