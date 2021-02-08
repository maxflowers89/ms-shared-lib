package com.illimity.rts.commonconfiglib.configs;

import com.illimity.rts.commonconfiglib.utilities.LoggingUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

@SuppressWarnings("EmptyMethod")
@Configuration
@Aspect
public class AOPLogger {

  private static final Logger LOG = LogManager.getLogger(AOPLogger.class);

  @Autowired
  private LoggingUtils loggingUtility;

  @Pointcut("execution(* com.illimity..*.*(..))")
  public void illimityClassesMonitoring() {
    // AOP empty method
  }

  @Pointcut("@within(org.springframework.web.bind.annotation.RestControllerAdvice))")
  public void restControllerAdviceMonitoring() {
    // AOP empty method
  }

  @Pointcut("@within(org.springframework.web.bind.annotation.ControllerAdvice))")
  public void controllerAdviceMonitoring() {
    // AOP empty method
  }

  @Pointcut("@within(org.springframework.web.bind.annotation.RestController))")
  public void restControllerMonitoring() {
    // AOP empty method
  }

  @Pointcut("execution(@com.illimity.rts.commonconfiglib.annotations.LogAroundThis * *(..))")
  public void loggingAnnotation() {
    // AOP empty method
  }

  @Before("(illimityClassesMonitoring() && restControllerMonitoring()) || loggingAnnotation()")
  public void initStartTime(JoinPoint joinPoint) {

    loggingUtility.buildInitialThreadContext(LOG.isDebugEnabled(), joinPoint);
  }

  @Around("(illimityClassesMonitoring() && restControllerMonitoring()) || loggingAnnotation()")
  public Object logAround(ProceedingJoinPoint pjp) throws Throwable {

    Object returnValue = pjp.proceed();

    loggingUtility.printLog(LOG, returnValue);

    return returnValue;

  }

  @AfterThrowing(pointcut = "restControllerMonitoring() || loggingAnnotation()", throwing = "ex")
  public void logError(JoinPoint jp, Exception ex) {

    loggingUtility.printExceptionLog(LOG, ex);
  }

  @AfterReturning(pointcut = "controllerAdviceMonitoring() || restControllerAdviceMonitoring() || loggingAnnotation()", returning = "returnValue")
  public void logAfterExceptionHandled(JoinPoint joinPoint, Object returnValue) {
    loggingUtility.printHandledExceptionLog(LOG, joinPoint, returnValue);
  }
}
