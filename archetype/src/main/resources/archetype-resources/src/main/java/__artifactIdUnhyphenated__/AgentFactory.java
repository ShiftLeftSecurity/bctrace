package ${package}.${artifactIdUnhyphenated};

import io.shiftleft.bctrace.Agent;

public class AgentFactory implements io.shiftleft.bctrace.AgentFactory {

  @Override
  public Agent createAgent() {
    return ${package}.${artifactIdUnhyphenated}.Agent.getInstance();
  }
}
