package com.illimity.rts.commonconfiglib.handlers.impl;

import com.illimity.rts.commonconfiglib.handlers.ICustomExceptionsHandler;
import com.illimity.rts.commonconfiglib.types.fe.errorresponses.ErrorResponse;
import com.illimity.rts.commonconfiglib.types.fe.exception.AbstractErrorException;
import com.illimity.rts.commonconfiglib.utilities.JsonUtils;
import com.mongodb.MongoException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import javax.servlet.http.HttpServletRequest;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestControllerAdvice
public class CustomExceptionsHandler
    extends ResponseEntityExceptionHandler
    implements ICustomExceptionsHandler {

  @Value("${spring.profiles.active}")
  protected String profile;

  private static final Logger LOG = LogManager.getLogger(CustomExceptionsHandler.class);

  @ExceptionHandler({AbstractErrorException.class})
  public ResponseEntity<ErrorResponse> abstractErrorExceptionExceptionHandler(
          AbstractErrorException ex, ServletWebRequest request
  ) {
    return new ResponseEntity<>(
            ICustomExceptionsHandler.buildErrorResponse(
                    profile,
                    request.getRequest(),
                    ex.getErrorCode().name(),
                    ex.getErrorCode().getDescription(),
                    ex.getMessage()
            ),
            ex.getHttpStatus());
  }

  @ExceptionHandler(CompletionException.class)
  public ResponseEntity<? extends ErrorResponse> handleCompletionException(
          Exception e,
          WebRequest request){
    if (AbstractErrorException.class.isAssignableFrom(e.getCause().getClass())){
      return abstractErrorExceptionExceptionHandler(
              ((AbstractErrorException) e.getCause()),
              (ServletWebRequest) request);
    }
    else { return handleGenericException(e,  ((ServletWebRequest) request).getRequest()); }
  }

  @Override
  public ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException manve,
                                                             @Nullable HttpHeaders headers,
                                                             HttpStatus status,
                                                             WebRequest request) {

    return new ResponseEntity<>(
        manve.getBindingResult().getFieldErrors()
            .stream()
            .filter(fieldError -> !StringUtils.isEmpty(fieldError.getDefaultMessage()))
            .distinct()
            .map(fieldError -> {
              StringBuilder message = new StringBuilder();

              LOG.warn(
                  message
                      .append("Error '")
                      .append(fieldError.getDefaultMessage())
                      .append("' on field ")
                      .append(fieldError.getField())
                      .append(" -> rejected value is '")
                      .append(JsonUtils.stringify(fieldError.getRejectedValue()))
                      .append("'")
                      .toString());

              return ICustomExceptionsHandler
                  .buildErrorResponse(
                      profile,
                      ((ServletWebRequest) request).getRequest(),
                      String.join("_",
                          fieldError.getField().substring(fieldError.getField().lastIndexOf(".") + 1),
                          fieldError.getCode()),
                      "Validation error on field " + fieldError.getField(),
                      message);
            }).collect(Collectors.toList()),
        HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(HttpStatusCodeException.class)
  public ResponseEntity<? extends ErrorResponse> handleHttpStatusCodeException(
      HttpStatusCodeException hsce, HttpServletRequest request) {

    return new ResponseEntity<>(
        ICustomExceptionsHandler.buildErrorResponse(
            profile,
            request,
            "HTTPERRCONN",
            "Http connection error",
            Stream
                .of(hsce.getMessage(), hsce.getResponseBodyAsString())
                .collect(Collectors.toList())),
        hsce.getStatusCode());
  }

  @ExceptionHandler(MongoException.class)
  public ResponseEntity<? extends ErrorResponse> handleMongoException(MongoException me,
                                                                      HttpServletRequest request) {
    return new ResponseEntity<>(
        ICustomExceptionsHandler.buildErrorResponse(
            profile,
            request,
            "DBERR",
            "Database error",
            me.getMessage()),
        HttpStatus.INTERNAL_SERVER_ERROR);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<? extends ErrorResponse> handleGenericException(Exception e,
                                                                        HttpServletRequest request) {
    return new ResponseEntity<>(
        ICustomExceptionsHandler.buildErrorResponse(
            profile,
            request,
            "INTSERVERERR",
            "Internal server error",
            e.getMessage()),
        HttpStatus.INTERNAL_SERVER_ERROR);
  }

  @Override
  protected ResponseEntity<Object> handleExceptionInternal(Exception e,
                                                           @Nullable Object body,
                                                           HttpHeaders headers,
                                                           HttpStatus status,
                                                           WebRequest request) {
    if (HttpStatus.INTERNAL_SERVER_ERROR.equals(status)) {
      request.setAttribute("javax.servlet.error.exception", e, 0);
    }

    return new ResponseEntity<>(
        ICustomExceptionsHandler.buildErrorResponse(
            profile,
            ((ServletWebRequest) request).getRequest(),
            "INTSERVERERR",
            "Internal server error",
            e.getMessage()),
        status);
  }
}
