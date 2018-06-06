package io.shiftleft.bctrace;

import io.shiftleft.bctrace.Bctrace;
import io.shiftleft.bctrace.hook.Hook;

public interface Agent {

  public void init(Bctrace bctrace);

  public void afterRegistration();

  public Hook[] getHooks();
}
