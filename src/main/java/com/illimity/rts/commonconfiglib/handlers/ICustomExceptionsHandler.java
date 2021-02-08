package com.illimity.rts.commonconfiglib.handlers;

import com.illimity.rts.commonconfiglib.types.fe.errorresponses.ErrorResponse;
import org.springframework.http.ResponseEntity;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

public interface ICustomExceptionsHandler {

  ResponseEntity<? extends ErrorResponse> handleGenericException(Exception e,
                                                                 HttpServletRequest request);

  static ErrorResponse buildErrorResponse(String profile, HttpServletRequest request,
                                          String errorCode, String errorMessage, Object errorDetails) {

    return ErrorResponse.builder()
        .timestamp(LocalDateTime.now().toInstant(ZoneOffset.UTC).toEpochMilli())
        .uri(request.getRequestURI())
        .pod(!"prod".equalsIgnoreCase(profile) ? request.getLocalName() : null)
        .errorCode(errorCode)
        .errorMessage(errorMessage)
        .technicalErrorDetails(!"prod".equalsIgnoreCase(profile) ? errorDetails : null)
        .build();
  }
}
