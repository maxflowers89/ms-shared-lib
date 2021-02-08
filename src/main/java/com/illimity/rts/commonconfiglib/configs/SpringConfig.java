package com.illimity.rts.commonconfiglib.configs;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class SpringConfig implements WebMvcConfigurer {

  @Value("${cors.mapping:/**}")
  private String mapping;
  @Value("${cors.allowed_methods:HEAD,GET,PUT,POST,DELETE,PATCH}")
  private String[] allowedMethods;

  @Override
  public void addCorsMappings(CorsRegistry registry) {
    registry.addMapping(mapping).allowedMethods(allowedMethods);
  }

}
