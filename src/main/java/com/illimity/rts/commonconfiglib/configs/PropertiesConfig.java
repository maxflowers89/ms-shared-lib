package com.illimity.rts.commonconfiglib.configs;

import com.illimity.rts.commonconfiglib.properties.JwtAuthPathProperties;
import com.illimity.rts.commonconfiglib.properties.LoggingProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({
    LoggingProperties.class,
    JwtAuthPathProperties.class
})
public class PropertiesConfig {
}
