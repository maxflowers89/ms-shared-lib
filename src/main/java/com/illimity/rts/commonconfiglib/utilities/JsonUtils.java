package com.illimity.rts.commonconfiglib.utilities;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.core.io.Resource;

public class JsonUtils {

  private static final Logger LOG = LogManager.getLogger(JsonUtils.class);

  private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder()
      .addModule(new JavaTimeModule())
      .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
      .build();

  public static String stringify(Object o) {
    try {
      return OBJECT_MAPPER.writeValueAsString(o);
    } catch (JsonProcessingException e) {
      LOG.error(e.getMessage(), e);
      return "Object deserialization error";
    }
  }

  public static <T> T asPojo(String resource, Class<T> clazz) {
    try {
      OBJECT_MAPPER.registerModule(new JavaTimeModule());
      return OBJECT_MAPPER.readValue(resource, clazz);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public static <T> T asPojo(Resource resource, Class<T> clazz) {
    try {
      return OBJECT_MAPPER.readValue(resource.getInputStream(), clazz);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public static <T> T asPojo(byte[] resource, Class<T> clazz) {
    try {
      return OBJECT_MAPPER.readValue(resource, clazz);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public static ObjectMapper objectMapper() {
    return OBJECT_MAPPER;
  }

}

