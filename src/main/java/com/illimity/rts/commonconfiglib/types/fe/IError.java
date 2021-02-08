package com.illimity.rts.commonconfiglib.types.fe;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

public interface IError {

  default long getTimestamp() {
    return Instant.now(Clock.system(ZoneOffset.UTC)).toEpochMilli();
  }

  default String getUri() {
    return null;
  }

  default String getPod() {
    return null;
  }

  String getErrorCode();

  String getErrorMessage();

  Object getTechnicalErrorDetails();
}
