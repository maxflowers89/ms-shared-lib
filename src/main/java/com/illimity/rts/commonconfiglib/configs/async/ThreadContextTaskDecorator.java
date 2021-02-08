package com.illimity.rts.commonconfiglib.configs.async;

import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.core.task.TaskDecorator;

import java.util.Map;

@Log4j2
public class ThreadContextTaskDecorator implements TaskDecorator {

  @Override
  public Runnable decorate(Runnable runnable) {
    Map<String, String> logContext = ThreadContext.getContext();

    return () -> {
      try {
        ThreadContext.putAll(logContext);

        runnable.run();
      } finally {
        ThreadContext.clearAll();
      }
    };
  }
}
