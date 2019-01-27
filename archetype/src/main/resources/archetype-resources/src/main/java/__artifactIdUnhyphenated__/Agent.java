package ${package}.${artifactIdUnhyphenated};

import io.shiftleft.bctrace.Bctrace;
import io.shiftleft.bctrace.hook.Hook;
import io.shiftleft.bctrace.logging.Level;
import io.shiftleft.bctrace.logging.Logger;
import ${package}.${artifactIdUnhyphenated}.hooks.StringCreationHook;

public class Agent implements io.shiftleft.bctrace.Agent {

  private static final Agent INSTANCE = new Agent();

  private static final Logger LOGGER = Bctrace.getAgentLogger();

  private final Hook[] hooks = new Hook[]{
      // TODO register your hooks here
      new StringCreationHook()
  };

  private Bctrace bctrace;

  @Override
  public void init(final Bctrace bctrace) {
    this.bctrace = bctrace;
  }

  @Override
  public void afterRegistration() {
    LOGGER.log(Level.INFO, "Starting bctrace agent ...");
  }

  public static Agent getInstance() {
    return INSTANCE;
  }

  public Bctrace getBctrace() {
    return bctrace;
  }

  @Override
  public Hook[] getHooks() {
    return this.hooks;
  }

  @Override
  public void showMenu() {
    System.out.println("aaaa");
  }
}
