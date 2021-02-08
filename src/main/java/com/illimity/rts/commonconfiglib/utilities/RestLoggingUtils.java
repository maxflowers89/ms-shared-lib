package com.illimity.rts.commonconfiglib.utilities;

import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.illimity.rts.commonconfiglib.utilities.CommonLoggingUtils.*;

@Log4j2
@Component
public class RestLoggingUtils {

  private Integer maxLogSize;

  @Value("${logging.param-black-list:}")
  private String[] paramBlackList;

  @Autowired
  public void setMaxLogSize(@Value("${logging.size:10000}") Integer maxLogSize) {
    this.maxLogSize = maxLogSize;
  }

  public void printRestTemplateLog(Logger LOG, HttpRequest request, long start,
                                   byte[] body, ClientHttpResponse response) throws IOException {
    buildHttpClientThreadContext(
        LOG.isDebugEnabled(),
        request.getMethodValue(),
        request.getURI().toASCIIString(),
        body,
        start,
        response.getStatusCode(),
        response.getBody());

    logHttpClientAndCleanThreadContext(
        LOG,
        request.getMethodValue(),
        request.getURI().toASCIIString(),
        response.getStatusCode());
  }

  protected void buildHttpClientThreadContext(boolean debug,
                                              String method,
                                              String url,
                                              byte[] requestBody,
                                              long start,
                                              HttpStatus httpStatus,
                                              InputStream responseBody) throws IOException {

    ThreadContext.put("rest.statusCode", httpStatus.name());
    ThreadContext.put("rest.requestMethod", method);
    ThreadContext.put("rest.url", url);

    List<String> completeParamBlackList = buildCompleteParamBlackList(paramBlackList);

    if (debug && requestBody.length > 0) {
      String request = new String(requestBody, StandardCharsets.UTF_8);

      try {
        request = manipulateJsonNode(
            JsonUtils.objectMapper().readTree(request),
            new JSONObject(),
            maxLogSize,
            completeParamBlackList).toString();
      } catch (Exception e) {
        request = request.length() > maxLogSize ?
            request.substring(0, maxLogSize) : request;
      }

      ThreadContext.put("rest.payload.request", request);
    }

    if (debug && responseBody.available() > 0) {
      String response = StreamUtils.copyToString(responseBody, StandardCharsets.UTF_8);

      try {
        response = manipulateJsonNode(
            JsonUtils.objectMapper().readTree(response),
            new JSONObject(),
            maxLogSize,
            completeParamBlackList).toString();
      } catch (Exception e) {
        response = response.length() > maxLogSize ?
            response.substring(0, maxLogSize) : response;
      }

      ThreadContext.put("rest.payload.response", response);
    }

    ThreadContext.put("rest.duration", buildDuration(start));
  }

  public void printOkHttpClientLog(Logger LOG, okhttp3.Request request, long start,
                                   byte[] body, okhttp3.Response response) throws IOException {
    buildHttpClientThreadContext(
        LOG.isDebugEnabled(),
        request.method(),
        request.url().uri().toASCIIString(),
        body,
        start,
        HttpStatus.valueOf(response.code()),
        response.body() != null ?
            response.peekBody(Long.MAX_VALUE).byteStream() :
            new ByteArrayInputStream(new byte[0]));

    logHttpClientAndCleanThreadContext(
        LOG,
        request.method(),
        request.url().uri().toASCIIString(),
        HttpStatus.valueOf(response.code()));
  }

  private void logHttpClientAndCleanThreadContext(Logger LOG, String requestMethod,
                                                  String url, HttpStatus httpStatus) {

    String startTime = ThreadContext.get("startTime");

    ThreadContext.remove("startTime");

    if (httpStatus.is4xxClientError()) {
      LOG.warn("{}: calling restful API {}",
          LocalDateTime.ofInstant(Instant.now(Clock.system(ZoneOffset.UTC)), ZoneOffset.UTC),
          "[" + requestMethod + "] " + url);
    } else if (httpStatus.is5xxServerError()) {
      LOG.error("{}: calling restful API {}",
          LocalDateTime.ofInstant(Instant.now(Clock.system(ZoneOffset.UTC)), ZoneOffset.UTC),
          "[" + requestMethod + "] " + url);
    } else {
      LOG.info("{}: calling restful API {}",
          LocalDateTime.ofInstant(Instant.now(Clock.system(ZoneOffset.UTC)), ZoneOffset.UTC),
          "[" + requestMethod + "] " + url);
    }

    ThreadContext.put("startTime", startTime);

    ThreadContext.removeAll(Stream
        .of("rest.statusCode",
            "rest.requestMethod",
            "rest.url",
            "rest.payload.request",
            "rest.payload.response",
            "rest.duration")
        .collect(Collectors.toList()));
  }
}
