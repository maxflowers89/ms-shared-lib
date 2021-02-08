package com.illimity.rts.commonconfiglib.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@ConfigurationProperties("jwt.auth.path")
public class JwtAuthPathProperties {

  private String whitelist;
}
