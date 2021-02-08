package com.illimity.rts.commonconfiglib;

import com.illimity.rts.commonconfiglib.types.logging.Parameter;
import com.illimity.rts.commonconfiglib.utilities.CommonLoggingUtils;
import com.illimity.rts.commonconfiglib.utilities.JsonUtils;
import lombok.extern.log4j.Log4j2;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

@Log4j2
@RunWith(SpringRunner.class)
public class CommonLoggingUtilsTest extends CommonLoggingUtils {

  @Value("classpath:long-response.json")
  private Resource longResponse;

  @Value("classpath:long-request-body.json")
  private Resource longRequest;

  @Test
  public void nullResponseTest() {

    assertNull(buildResponse(null, 1000, new String[]{}));
  }

  @Test
  public void manipulateLongResponseTest() {

    String response = buildResponse(JsonUtils.asPojo(longResponse, Object.class), 1000, new String[]{});

    assertNotNull(response);

    JSONObject json = new JSONObject(response);

    assertTrue(json.has("password"));
    assertThat(json.getString("password"), is("<hidden>"));
    assertTrue(json.has("oldPasswords"));
    assertThat(json.getString("oldPasswords"), is("<hidden>"));
    assertTrue(json.has("onboardingTrackingId"));
    assertThat(json.getString("onboardingTrackingId"), is("1605180610156665368677057684037"));
    assertTrue(json.has("transactionRequestId"));
    assertThat(json.getString("transactionRequestId"), is("1605180610156665368677057684037"));
    assertTrue(json.has("barCode"));
    assertThat(json.getString("barCode"), is("null"));
    assertTrue(json.has("documentType"));
    assertThat(json.getString("documentType"), is("drivingLicense"));
    assertTrue(json.has("typeOfUpload"));
    assertThat(json.getString("typeOfUpload"), is("manual"));
    assertTrue(json.has("boolean"));
    assertTrue(json.getBoolean("boolean"));
    assertTrue(json.has("integer"));
    assertEquals(23408, json.getInt("integer"));
    assertTrue(json.has("floating"));
    assertThat(json.getDouble("floating"), is(56.98));
    assertTrue(json.has("list"));
    assertEquals(json.getJSONArray("list").length(), 4);
    assertTrue(json.has("objectList"));
    assertEquals(json.getJSONArray("objectList").length(), 2);
    assertTrue(json.has("frontImage"));
    assertTrue(json.getJSONObject("frontImage").has("filename"));
    assertTrue(json.getJSONObject("frontImage").has("data"));
    assertTrue(json.getJSONObject("frontImage").getString("data").endsWith("..."));
  }

  @Test
  public void manipulateLongRequestTest() {

    Parameter notAllowedParam = buildRequestParameter(
        JsonUtils.asPojo(longRequest, Object.class),
        () -> RequestParam.class,
        "password",
        1000,
        1,
        new String[]{});

    assertEquals("password", notAllowedParam.getName());
    assertEquals("@RequestParam", notAllowedParam.getType());
    assertEquals("<hidden>", notAllowedParam.getValue());

    Parameter requestBody = buildRequestParameter(
        JsonUtils.asPojo(longRequest, Object.class),
        () -> RequestBody.class,
        "name",
        1000,
        1,
        new String[]{});

    assertEquals("name", requestBody.getName());
    assertEquals("@RequestBody", requestBody.getType());
    assertNotNull(requestBody.getValue());

    Parameter pathVariable = buildRequestParameter(
        true,
        () -> PathVariable.class,
        "path",
        1000,
        1,
        new String[]{});

    assertEquals("path", pathVariable.getName());
    assertEquals("@PathVariable", pathVariable.getType());
    assertEquals("true", String.valueOf(pathVariable.getValue()));

    Parameter requestParameter = buildRequestParameter(
        "requestParam",
        () -> RequestParam.class,
        "param",
        1000,
        1,
        new String[]{});

    assertEquals("param", requestParameter.getName());
    assertEquals("@RequestParam", requestParameter.getType());
    assertEquals("requestParam", requestParameter.getValue());

    Parameter nullParameter = buildRequestParameter(
        null,
        () -> RequestParam.class,
        "nullParam",
        1000,
        1,
        new String[]{});

    assertEquals("nullParam", nullParameter.getName());
    assertEquals("@RequestParam", nullParameter.getType());
    assertNull(nullParameter.getValue());

    Parameter notPrimitiveParameter = buildRequestParameter(
        Long.valueOf("56"),
        () -> RequestParam.class,
        "notPrimitiveParam",
        1000,
        1,
        new String[]{});

    assertEquals("notPrimitiveParam", notPrimitiveParameter.getName());
    assertEquals("@RequestParam", notPrimitiveParameter.getType());
    assertEquals("56", notPrimitiveParameter.getValue());

    Parameter nullRequestBody = buildRequestParameter(
        null,
        () -> RequestBody.class,
        "nullRequestBody",
        1000,
        1,
        new String[]{});

    assertEquals("nullRequestBody", nullRequestBody.getName());
    assertEquals("@RequestBody", nullRequestBody.getType());
    assertNull(nullRequestBody.getValue());

    String params = JsonUtils
        .stringify(Stream
            .of(requestBody, pathVariable, requestParameter, nullParameter, notPrimitiveParameter, nullRequestBody)
            .collect(Collectors.toList()));

    assertFalse(params.replace("\\", "").contains("\\"));
    assertFalse(params.replace("\\", "").contains("\"\""));
  }

  @Test
  public void restTemplateCallTest() {

  }
}
