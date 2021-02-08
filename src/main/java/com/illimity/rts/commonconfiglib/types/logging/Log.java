/*
 * Copyright (C) 2018-2019 illimity
 */
package com.illimity.rts.commonconfiglib.types.logging;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Log {

  @JsonProperty("x_http_response_x_user_trace_id")
  private String userId;
  @JsonProperty("x_http_response_x_server_session_trace_id")
  private String sessionId;
  @JsonProperty("x_http_response_x_tx_trace_id")
  private String traceId;
  private String clt;
  private System system;
  private long requestTime;
  private Http http;
  private String calledMethod;
  private Payload payload;
  private String exception;
  private String exceptionMessage;
  private String cause;
  private String causeMessage;
  private Long duration;
  private String pod_name;
  private Network network;
  private String podIpAddress;
}
