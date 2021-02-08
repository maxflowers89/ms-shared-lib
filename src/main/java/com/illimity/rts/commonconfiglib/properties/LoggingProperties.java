package com.illimity.rts.commonconfiglib.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@ConfigurationProperties("logging")
public class LoggingProperties {

  private String root;
  private String app;
  private String log4j2Layout;
  private String logger;
}
