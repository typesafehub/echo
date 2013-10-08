package com.typesafe.atmos.trace;

import play.core.Execution;
import scala.concurrent.ExecutionContext;

// Needed to get at the play.core.Execution.internalContext value.  Marked private in Scala
public class ExecutionInitializer {
  public final static ExecutionContext internalContext() {
    return Execution.internalContext();
  }
}