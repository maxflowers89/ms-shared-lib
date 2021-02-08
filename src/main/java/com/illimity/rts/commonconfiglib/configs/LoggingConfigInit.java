package com.illimity.rts.commonconfiglib.configs;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.Order;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilderFactory;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;
import org.apache.logging.log4j.core.config.plugins.Plugin;

@Plugin(
    name = "CustomConfigurationFactory",
    category = ConfigurationFactory.CATEGORY)
@Order(50)
public class LoggingConfigInit extends ConfigurationFactory {

  private static final String JSON_APPENDER_NAME = "STDOUT_json";
  private static final String PLAIN_APPENDER_NAME = "STDOUT_plain";

  public static final String BASE_LOGGER = "com.illimity.rts.commonconfiglib";

  public String[] getSupportedTypes() {
    return new String[]{"*"};
  }

  @Override
  public Configuration getConfiguration(LoggerContext loggerContext, ConfigurationSource configurationSource) {
    return configuration().build();
  }

  private ConfigurationBuilder<BuiltConfiguration> configuration() {
    ConfigurationBuilder<BuiltConfiguration> builder = ConfigurationBuilderFactory
        .newConfigurationBuilder();

    builder
        .setStatusLevel(Level.WARN)
        .setConfigurationName("base-illimity-config")
        .setMonitorInterval(String.valueOf(300))
        .add(builder
            .newAppender(JSON_APPENDER_NAME, "Console")
            .addComponent(builder
                .newLayout("JsonLayout")
                .addAttribute("compact", true)
                .addAttribute("eventEol", true)
                .addAttribute("properties", true)))
        .add(builder
            .newAppender(PLAIN_APPENDER_NAME, "Console")
            .addComponent(builder
                .newLayout("PatternLayout")
                .addAttribute(
                    "pattern",
                    "{%d [%t] %-5level: %m%X%n%throwable}")))
        .add(builder
            .newRootLogger(Level.WARN)
            .addAttribute("additivity", false)
            .add(builder.newAppenderRef(JSON_APPENDER_NAME)))
        .add(builder
            .newLogger(BASE_LOGGER, Level.INFO)
            .addAttribute("additivity", false)
            .add(builder.newAppenderRef(JSON_APPENDER_NAME)));

    return builder;
  }
}
