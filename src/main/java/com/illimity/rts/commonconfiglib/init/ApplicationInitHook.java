package com.illimity.rts.commonconfiglib.init;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.AppenderRef;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;

import java.util.Optional;

import static com.illimity.rts.commonconfiglib.configs.LoggingConfigInit.BASE_LOGGER;

public interface ApplicationInitHook {

  static void addInitHooks(SpringApplication application) {
    application.addListeners((ApplicationListener<ApplicationEnvironmentPreparedEvent>) event -> {

      final LoggerContext ctx = (LoggerContext) LogManager.getContext(false);

      String appenderName = "STDOUT_" +
          Optional
              .ofNullable(event.getEnvironment().getProperty("logging.log4j2-layout"))
              .orElse("json");

      AppenderRef appenderRef = AppenderRef.createAppenderRef(appenderName, null, null);

      String logger = event.getEnvironment().getProperty("logging.logger-name");

      // if specified app specific logger instantiation
      if (logger != null) {
        LoggerConfig loggerConfig = LoggerConfig.createLogger(
            false,
            Level.getLevel(Optional
                .ofNullable(event.getEnvironment().getProperty("logging.app"))
                .orElse("INFO")),
            logger,
            "true",
            new AppenderRef[]{appenderRef},
            null,
            ctx.getConfiguration(),
            null);

        loggerConfig.addAppender(ctx.getConfiguration().getAppender(appenderName), null, null);

        ctx.getConfiguration().addLogger(logger, loggerConfig);
      }

      // root logger appender rewrite, if necessary
      setLoggerConfig(
          ctx.getConfiguration(),
          ctx.getConfiguration().getRootLogger(),
          appenderName,
          Optional
              .ofNullable(event.getEnvironment().getProperty("logging.root"))
              .orElse("WARN"));

      // base logger appender rewrite, if necessary
      setLoggerConfig(
          ctx.getConfiguration(),
          ctx.getConfiguration().getLoggerConfig(BASE_LOGGER),
          appenderName,
          Optional
              .ofNullable(event.getEnvironment().getProperty("logging.app"))
              .orElse("INFO"));

      ctx.updateLoggers();
    });
  }

  private static void setLoggerConfig(Configuration conf, LoggerConfig logger, String appenderName, String level) {
    if (!logger.getAppenders().containsKey(appenderName)) {
      logger.getAppenders().keySet()
          .parallelStream()
          .forEach(logger::removeAppender);

      logger.addAppender(conf.getAppender(appenderName), null, null);
    }

    logger.setLevel(Level.getLevel(level));
  }
}
