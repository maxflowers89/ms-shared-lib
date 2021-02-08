package com.illimity.rts.commonconfiglib.types.logging;

import org.apache.logging.log4j.Level;

public enum LogLevels {
  OFF(Level.OFF),
  FATAL(Level.FATAL),
  ERROR(Level.ERROR),
  WARN(Level.WARN),
  INFO(Level.INFO),
  DEBUG(Level.DEBUG),
  TRACE(Level.TRACE),
  ALL(Level.ALL);

  private Level level;

  LogLevels(Level level) {
    this.level = level;
  }

  public Level getLevel() {
    return level;
  }
}
