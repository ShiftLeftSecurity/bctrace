package io.shiftleft.bctrace.spi;

import io.shiftleft.bctrace.Bctrace;

public interface Agent {

  public void init(Bctrace bctrace);

  public void afterRegistration();

  public Hook[] getHooks();
}
