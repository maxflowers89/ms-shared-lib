package com.illimity.rts.commonconfiglib.types.fe.exception;

import com.illimity.rts.commonconfiglib.types.fe.IErrorCode;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.http.HttpStatus;

@Data
@EqualsAndHashCode(callSuper = true)
public class AbstractErrorException extends RuntimeException {
  private final transient IErrorCode errorCode;
  private final HttpStatus httpStatus;

  public AbstractErrorException(String message, HttpStatus httpStatus, IErrorCode errorCode) {
    super(message);
    this.httpStatus = httpStatus;
    this.errorCode = errorCode;
  }
}
