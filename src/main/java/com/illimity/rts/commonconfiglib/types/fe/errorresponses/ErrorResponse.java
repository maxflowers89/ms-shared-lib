package com.illimity.rts.commonconfiglib.types.fe.errorresponses;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.illimity.rts.commonconfiglib.types.fe.IError;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse implements IError {
  private long timestamp;
  private String uri;
  private String pod;
  private String errorCode;
  private String errorMessage;
  private Object technicalErrorDetails;
}
