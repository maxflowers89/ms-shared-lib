package com.illimity.rts.commonconfiglib.configs.resttemplate;

import com.illimity.rts.commonconfiglib.utilities.RestLoggingUtils;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;

@Log4j2
@AllArgsConstructor
public class CustomClientHttpRequestInterceptor implements ClientHttpRequestInterceptor {

  private static final String TRACE_ID_HEADER_NAME = "X-Tx-Trace-Id";
  private static final String TRACE_ID = "traceId";

  private RestLoggingUtils loggingUtility;


  @Override
  public ClientHttpResponse intercept(HttpRequest httpRequest,
                                      byte[] body,
                                      ClientHttpRequestExecution clientHttpRequestExecution)
      throws IOException {

    httpRequest.getHeaders().set(TRACE_ID_HEADER_NAME, ThreadContext.get(TRACE_ID));

    long start = System.nanoTime();

    ClientHttpResponse response = clientHttpRequestExecution.execute(httpRequest, body);

    loggingUtility.printRestTemplateLog(
        log,
        httpRequest,
        start,
        body,
        response);

    return response;
  }
}
