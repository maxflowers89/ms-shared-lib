package com.illimity.rts.commonconfiglib.utilities;

import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.aspectj.lang.JoinPoint;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.illimity.rts.commonconfiglib.utilities.CommonLoggingUtils.*;

@Log4j2
@Component
public class LoggingUtils {

  @Value("${logging.size:10000}")
  private Integer maxLogSize;

  @Value("${logging.param-black-list:}")
  private String[] paramBlackList;

  public void buildInitialThreadContext(boolean debug, JoinPoint jp) {
    ThreadContext.put("startTime", String.valueOf(System.nanoTime()));

    ThreadContext.put("controller.method", jp.getSignature().getDeclaringTypeName() + "." + jp.getSignature().getName());

    if (debug && jp.getArgs().length > 0) {
      try {
        ThreadContext.put(
            "controller.payload.request",
            buildRequest(jp, maxLogSize, paramBlackList));
      } catch (Exception e) {
        log.error("Error while build controller request. Fix on Common Config Lib is needed {}", e.getMessage(), e);
      }
    }
  }

  public void printLog(Logger LOG, Object returnValue) {

    buildReturnThreadContext(LOG.isDebugEnabled(), returnValue);

    LOG.info("{}: received request on API {}",
        LocalDateTime.ofInstant(Instant.now(Clock.system(ZoneOffset.UTC)), ZoneOffset.UTC),
        buildRequestUrl(((ServletRequestAttributes) RequestContextHolder
            .currentRequestAttributes())
            .getRequest()));

    cleanControllerThreadContext();
  }

  public void printHandledExceptionLog(Logger LOG, JoinPoint jp, Object returnValue) {

    buildReturnThreadContext(LOG.isDebugEnabled(), returnValue);

    Throwable e = null;
    for (Object arg : jp.getArgs()) {
      if (arg instanceof Throwable) {
        e = (Throwable) arg;
        break;
      }
    }
    HttpStatus status;
    try {
      status = HttpStatus.valueOf(Optional.ofNullable(
          ThreadContext.get("controller.httpStatus")).orElse("Invalid"));
    } catch (IllegalArgumentException ex) {
      status = HttpStatus.INTERNAL_SERVER_ERROR;
    }
    if (status.is5xxServerError()) {
      LOG.error("{}: error while executing API {}",
          LocalDateTime.ofInstant(Instant.now(Clock.system(ZoneOffset.UTC)), ZoneOffset.UTC),
          buildRequestUrl(((ServletRequestAttributes) RequestContextHolder
              .currentRequestAttributes())
              .getRequest()),
          e);
    } else {
      LOG.warn("{}: error while executing API {}",
          LocalDateTime.ofInstant(Instant.now(Clock.system(ZoneOffset.UTC)), ZoneOffset.UTC),
          buildRequestUrl(((ServletRequestAttributes) RequestContextHolder
              .currentRequestAttributes())
              .getRequest()),
          e);
    }

    cleanControllerThreadContext();
  }

  public void printExceptionLog(Logger LOG, Exception e) {

    buildExceptionThreadContext(LOG.isDebugEnabled(), e);
  }

  private void buildReturnThreadContext(boolean debug, Object returnValue) {

    if (debug && returnValue != null) {
      try {
        ThreadContext.put("controller.payload.response", buildResponse(returnValue, maxLogSize, paramBlackList));
      } catch (Exception e) {
        log.error("Error while build controller response. Fix on Common Config Lib is needed {}", e.getMessage(), e);
      }
    }
    if (returnValue instanceof ResponseEntity) {
      ResponseEntity responseEntity = (ResponseEntity) returnValue;

      ThreadContext.put("controller.httpStatus", responseEntity.getStatusCode().name());
    } else {
      HttpServletResponse response = ((ServletRequestAttributes) RequestContextHolder
          .currentRequestAttributes()).getResponse();
      if (response != null) {
        ThreadContext.put("controller.httpStatus", HttpStatus.valueOf(response.getStatus()).name());
      }
    }
    if (ThreadContext.containsKey("startTime")) {
      ThreadContext.put("controller.duration", buildDuration(Long.valueOf(ThreadContext.get("startTime"))));
      ThreadContext.remove("startTime");
    }
  }

  private void buildExceptionThreadContext(boolean debug, Exception e) {

    buildReturnThreadContext(debug, null);

    ThreadContext.put("controller.exception", e.getClass().getCanonicalName());
    ThreadContext.put("controller.exceptionMessage", buildExceptionMessage(e));

    if (e.getCause() != null) {
      ThreadContext.put("controller.cause", e.getCause().getClass().getCanonicalName());
      ThreadContext.put("controller.causeMessage", buildExceptionMessage(e.getCause()));
    }
  }

  private String buildRequestUrl(HttpServletRequest request) {
    StringBuilder requestBuilder = new StringBuilder();

    requestBuilder
        .append("[").append(request.getMethod()).append("]")
        .append(" ")
        .append(request.getRequestURL());

    if (request.getQueryString() != null) {
      requestBuilder.append("?").append(request.getQueryString());
    }

    return requestBuilder.toString();
  }

  private void cleanControllerThreadContext() {
    ThreadContext.removeAll(Stream
        .of("controller.method",
            "controller.payload.request",
            "controller.payload.response",
            "controller.httpStatus",
            "controller.exception",
            "controller.exceptionMessage",
            "controller.cause",
            "controller.causeMessage",
            "controller.duration")
        .collect(Collectors.toList()));
  }

  protected String buildExceptionMessage(Throwable t) {
    if (HttpStatusCodeException.class.isAssignableFrom(t.getClass())) {
      return ((HttpStatusCodeException) t).getResponseBodyAsString();
    }

    return t.getMessage();
  }
}
