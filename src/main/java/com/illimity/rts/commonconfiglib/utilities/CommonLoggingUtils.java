package com.illimity.rts.commonconfiglib.utilities;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ContainerNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.illimity.rts.commonconfiglib.types.logging.Parameter;
import com.jayway.jsonpath.JsonPath;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.core.io.AbstractResource;
import org.springframework.core.io.InputStreamSource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import javax.validation.Valid;
import java.io.File;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static com.illimity.rts.commonconfiglib.utilities.StreamCustomUtils.distinctByKey;

@Log4j2
public class CommonLoggingUtils {

  private static final String[] DEFAULT_PARAM_BLACK_LIST = {"password", "oldPasswords"};

  static String buildDuration(Long startTime) {
    return startTime != null ?
        Long.toString(TimeUnit.NANOSECONDS.toMillis(java.lang.System.nanoTime() - startTime)) :
        null;
  }

  static String buildRequest(JoinPoint jp, int maxLogSize, String[] paramBlackList) throws NoSuchMethodException {
    MethodSignature methodSignature = (MethodSignature) jp.getSignature();
    Method method = methodSignature.getMethod();

    Annotation[][] parameterAnnotations = jp
        .getTarget()
        .getClass()
        .getMethod(
            method.getName(),
            method.getParameterTypes())
        .getParameterAnnotations();

    String[] parameterNames = methodSignature.getParameterNames();

    return JsonUtils
        .stringify(IntStream.range(0, parameterAnnotations.length)
            .parallel()
            .mapToObj(i -> {
              Annotation[] annotations = parameterAnnotations[i];

              if (annotations.length == 0) {
                return Stream
                    .of(buildRequestParameter(
                        jp.getArgs()[i],
                        parameterNames[i],
                        maxLogSize,
                        parameterNames.length,
                        paramBlackList))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
              } else {
                return Stream
                    .of(annotations)
                    .parallel()
                    .filter(annotation -> !annotation.annotationType().isAssignableFrom(RequestHeader.class)
                        && !annotation.annotationType().isAssignableFrom(Valid.class))
                    .map(annotation -> buildRequestParameter(
                        jp.getArgs()[i],
                        annotation,
                        parameterNames[i],
                        maxLogSize,
                        parameterNames.length,
                        paramBlackList))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
              }
            })
            .flatMap(Collection::parallelStream)
            .filter(distinctByKey(Parameter::getName))
            .collect(Collectors.toList()))
        .replace("\\", "");
  }

  private static Parameter buildRequestParameter(Object arg,
                                                 String parameterName,
                                                 int maxLogSize,
                                                 int parameterNumber,
                                                 String[] paramBlackList) {

    return buildRequestParameter(arg, null, parameterName, maxLogSize, parameterNumber, paramBlackList);
  }

  protected static Parameter buildRequestParameter(Object arg,
                                                   Annotation annotation,
                                                   String parameterName,
                                                   int maxLogSize,
                                                   int parameterNumber,
                                                   String[] paramBlackList) {

    List<String> completeParamBlackList = buildCompleteParamBlackList(paramBlackList);

    if (!completeParamBlackList.contains(parameterName)) {
      String paramValue = null;

      if (arg != null) {

        if (byte[].class == arg.getClass() ||
            InputStreamSource.class.isAssignableFrom(arg.getClass()) ||
            File.class == arg.getClass()) {

          paramValue = "FILE_OR_BYTE_ARRAY_VALUE";
        } else {
          if (annotation.annotationType().isAssignableFrom(RequestBody.class)) {
            try {
              paramValue = manipulateJsonNode(
                  JsonUtils.objectMapper().readTree(JsonUtils.stringify(arg)),
                  new JSONObject(),
                  maxLogSize,
                  completeParamBlackList).toString();
            } catch (JsonProcessingException e) {
              log.warn("Error while trying to manipulate a very long response {}", e.getMessage(), e);
            }
          } else {
            int parameterLogSize = maxLogSize > 0 ? maxLogSize / parameterNumber : 0;

            if (arg instanceof String) {
              paramValue = parameterLogSize > 0 && ((String) arg).length() > parameterLogSize ?
                  ((String) arg).substring(0, parameterLogSize) :
                  (String) arg;
            } else {
              paramValue = String.valueOf(arg);
            }
          }
        }
      }

      return Parameter.builder()
          .type(annotation != null ? "@" + annotation.annotationType().getSimpleName() : null)
          .name(parameterName)
          .value(paramValue)
          .build();
    }

    return Parameter.builder()
        .type(annotation != null ? "@" + annotation.annotationType().getSimpleName() : null)
        .name(parameterName)
        .value("<hidden>")
        .build();
  }

  protected static String buildResponse(Object returnValue, int maxLogSize, String[] paramBlackList) {

    if (returnValue instanceof InputStream ||
        (returnValue instanceof ResponseEntity && ((ResponseEntity) returnValue).getBody() instanceof AbstractResource)) {
      return "STREAM_RET_VALUE";
    }

    if (returnValue != null) {
      String response = JsonUtils.stringify(returnValue);

      try {
        return manipulateJsonNode(
            JsonUtils.objectMapper().readTree(response),
            new JSONObject(),
            maxLogSize,
            buildCompleteParamBlackList(paramBlackList)).toString();
      } catch (Exception e) {
        log.warn("Error while trying to manipulate a very long response {}", e.getMessage(), e);

        return response;
      }
    }

    return null;
  }

  static Object manipulateJsonNode(JsonNode node,
                                   JSONObject json,
                                   int maxLogSize,
                                   List<String> completeParamBlackList) {

    if (node != null) {
      ContainerNode containerNode = JsonPath.read(node, "$");

      if (containerNode.isObject()) {
        return manipulateJsonNode((ObjectNode) containerNode, node, json, maxLogSize, completeParamBlackList);
      } else if (containerNode.isArray()) {
        JSONArray array = new JSONArray();

        for (int i = 0; i < containerNode.size(); i++) {
          array.put(manipulateJsonNode(node.get(i), json, maxLogSize, completeParamBlackList));
        }

        return array;
      }
    }

    return null;
  }

  private static JSONObject manipulateJsonNode(ObjectNode objectNode,
                                               JsonNode node,
                                               JSONObject json,
                                               int maxLogSize,
                                               List<String> completeParamBlackList) {
    int nodeLogSize = objectNode.size() > 0 ? maxLogSize / objectNode.size() : maxLogSize;

    node.fieldNames().forEachRemaining(fieldName -> {

      JsonNode subNode = node.get(fieldName);

      if (!completeParamBlackList.contains(fieldName)) {

        switch (subNode.getNodeType()) {
          case STRING:
            json.put(
                fieldName,
                !StringUtils.isEmpty(subNode.asText()) &&
                    subNode.asText().length() > nodeLogSize ?
                    subNode.asText().substring(0, nodeLogSize) + "..." :
                    subNode.asText());
            break;
          case ARRAY:
            subNode.forEach(elementNode ->
                json.append(
                    fieldName,
                    elementNode.isObject() ?
                        manipulateJsonNode(
                            elementNode,
                            new JSONObject(),
                            maxLogSize,
                            completeParamBlackList) :
                        getSimpleNode(elementNode)));
            break;
          case OBJECT:
            json.put(fieldName, manipulateJsonNode(subNode, new JSONObject(), maxLogSize, completeParamBlackList));
            break;
          case BOOLEAN:
            json.put(fieldName, subNode.asBoolean());
            break;
          case NUMBER:
            if (subNode.isInt()) {
              json.put(fieldName, subNode.asLong());
            } else {
              json.put(fieldName, subNode.asDouble());
            }
            break;
          default:
            json.put(fieldName, subNode);
        }
      } else {
        json.put(fieldName, "<hidden>");
      }
    });

    return json;
  }

  private static Object getSimpleNode(JsonNode node) {
    switch (node.getNodeType()) {
      case STRING:
        return node.asText();
      case BOOLEAN:
        return node.asBoolean();
      case NUMBER:
        if (node.isInt()) {
          return node.asLong();
        } else {
          return node.asDouble();
        }
    }

    return node;
  }

  static List<String> buildCompleteParamBlackList(String[] paramBlackList) {

    List<String> serviceBlackList = paramBlackList != null ?
        Arrays
            .asList(paramBlackList)
            .parallelStream()
            .peek(param -> param = param.strip())
            .collect(Collectors.toList()) :
        Collections.emptyList();

    return Stream
        .concat(
            Arrays.asList(DEFAULT_PARAM_BLACK_LIST).parallelStream(),
            serviceBlackList.parallelStream())
        .collect(Collectors.toList());
  }
}
