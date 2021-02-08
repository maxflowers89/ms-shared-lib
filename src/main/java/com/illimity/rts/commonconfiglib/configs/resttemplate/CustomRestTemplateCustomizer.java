package com.illimity.rts.commonconfiglib.configs.resttemplate;

import com.illimity.rts.commonconfiglib.utilities.RestLoggingUtils;
import lombok.AllArgsConstructor;
import org.springframework.boot.web.client.RestTemplateCustomizer;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@AllArgsConstructor
public class CustomRestTemplateCustomizer implements RestTemplateCustomizer {

  private RestLoggingUtils loggingUtility;

  @Override
  public void customize(RestTemplate restTemplate) {
    restTemplate.setRequestFactory(new BufferingClientHttpRequestFactory(restTemplate.getRequestFactory()));
    restTemplate.getInterceptors().add(new CustomClientHttpRequestInterceptor(loggingUtility));
  }
}
