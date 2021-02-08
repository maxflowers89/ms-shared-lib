package com.illimity.rts.commonconfiglib.configs.async;

import lombok.extern.log4j.Log4j2;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;

import java.lang.reflect.Method;

@Log4j2
public class CustomAsyncExceptionHandler implements AsyncUncaughtExceptionHandler {

  @Override
  public void handleUncaughtException(Throwable throwable, Method method, Object... objects) {
    log.warn("Error while executing async method {}: {}", method.getName(), throwable.getMessage(), throwable);
  }
}
