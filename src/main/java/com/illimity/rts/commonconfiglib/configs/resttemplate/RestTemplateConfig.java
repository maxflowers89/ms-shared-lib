package com.illimity.rts.commonconfiglib.configs.resttemplate;

import com.illimity.rts.commonconfiglib.utilities.RestLoggingUtils;
import org.apache.http.impl.client.CloseableHttpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {

  @Autowired
  private RestLoggingUtils restLoggingUtils;

  @Autowired
  private CloseableHttpClient httpClient;

  @Bean
  public RestTemplate restTemplate(RestTemplateBuilder restTemplateBuilder) {
    return restTemplateBuilder
        .requestFactory(this::clientHttpRequestFactory)
        .additionalCustomizers(customRestTemplateCustomizer(restLoggingUtils))
        .build();
  }

  @Bean
  public CustomRestTemplateCustomizer customRestTemplateCustomizer(RestLoggingUtils restLoggingUtils) {
    return new CustomRestTemplateCustomizer(restLoggingUtils);
  }

  @Bean
  public HttpComponentsClientHttpRequestFactory clientHttpRequestFactory() {
    HttpComponentsClientHttpRequestFactory clientHttpRequestFactory = new HttpComponentsClientHttpRequestFactory();
    clientHttpRequestFactory.setHttpClient(httpClient);
    return clientHttpRequestFactory;
  }
}
