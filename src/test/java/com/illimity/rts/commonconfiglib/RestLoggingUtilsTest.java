package com.illimity.rts.commonconfiglib;

import com.illimity.rts.commonconfiglib.utilities.RestLoggingUtils;
import org.apache.logging.log4j.ThreadContext;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDate;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;


@RunWith(SpringRunner.class)
public class RestLoggingUtilsTest extends RestLoggingUtils {

  @Value("classpath:long-response.json")
  private Resource longResponse;

  @Value("classpath:long-request-body.json")
  private Resource longRequest;

  @Before
  public void init() {
    ThreadContext.clearAll();
  }

  @Test
  public void restTemplateLogging_WithStringAsRequestAndResponseBodyTest() throws IOException {
    buildHttpClientThreadContext(
        true,
        "method",
        "url",
        "request_body_as_string".getBytes(),
        LocalDate.now().toEpochDay(),
        HttpStatus.BAD_REQUEST,
        new ByteArrayInputStream("response_body_as_string".getBytes()));

    assertThat(ThreadContext.get("rest.statusCode"), is(HttpStatus.BAD_REQUEST.name()));
    assertThat(ThreadContext.get("rest.requestMethod"), is("method"));
    assertThat(ThreadContext.get("rest.url"), is("url"));
    assertThat(ThreadContext.get("rest.payload.request"), is("request_body_as_string"));
    assertThat(ThreadContext.get("rest.payload.response"), is("response_body_as_string"));
    assertNotNull(ThreadContext.get("rest.duration"));
  }

  @Test
  public void restTemplateLogging_WithLongRequestAndResponseBodyTest() throws IOException {
    buildHttpClientThreadContext(
        true,
        "method",
        "url",
        longRequest.getInputStream().readAllBytes(),
        LocalDate.now().toEpochDay(),
        HttpStatus.BAD_REQUEST,
        new ByteArrayInputStream(longResponse.getInputStream().readAllBytes()));

    assertThat(ThreadContext.get("rest.statusCode"), is(HttpStatus.BAD_REQUEST.name()));
    assertThat(ThreadContext.get("rest.requestMethod"), is("method"));
    assertThat(ThreadContext.get("rest.url"), is("url"));

    System.out.println("Request: " + ThreadContext.get("rest.payload.request"));
    assertNotNull(ThreadContext.get("rest.payload.request"));

    System.out.println("Response: " + ThreadContext.get("rest.payload.response"));
    assertNotNull(ThreadContext.get("rest.payload.response"));

    assertNotNull(ThreadContext.get("rest.duration"));
  }

  @Test
  public void restTemplateLogging_WithNullRequestAndResponseTest() throws IOException {
    buildHttpClientThreadContext(
        true,
        "method",
        "url",
        new byte[]{},
        LocalDate.now().toEpochDay(),
        HttpStatus.BAD_REQUEST,
        new ByteArrayInputStream(new byte[]{}));

    assertThat(ThreadContext.get("rest.statusCode"), is(HttpStatus.BAD_REQUEST.name()));
    assertThat(ThreadContext.get("rest.requestMethod"), is("method"));
    assertThat(ThreadContext.get("rest.url"), is("url"));
    assertNull(ThreadContext.get("rest.payload.request"));
    assertNull(ThreadContext.get("rest.payload.response"));
    assertNotNull(ThreadContext.get("rest.duration"));
  }
}
