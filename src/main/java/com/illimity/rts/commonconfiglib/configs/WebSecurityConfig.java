package com.illimity.rts.commonconfiglib.configs;

import com.illimity.rts.commonconfiglib.properties.JwtAuthPathProperties;
import com.illimity.rts.jwt.auth.JwtAuthenticationFilter;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.stream.Stream;

@Configuration
@EnableWebSecurity
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

  @Autowired
  private JwtAuthenticationFilter jwtAuthenticationFilter;

  @Autowired
  private JwtAuthPathProperties jwtAuthPathProperties;

  private final String[] springSecurityWhiteList = {"/actuator/health"};
  private final String[] swaggerWhiteList = {
      "/swagger-ui.html",
      "/swagger-ui/**",
      "/v3/api-docs/**"};

  @Override
  protected void configure(HttpSecurity http) throws Exception {
    http
        .addFilterBefore(jwtAuthenticationFilter, BasicAuthenticationFilter.class)
        .csrf().disable()
        .sessionManagement()
        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        .and()
        .cors()
        .and()
        .authorizeRequests()
        .antMatchers(buildWhitelist()).permitAll()
        .anyRequest().authenticated();
  }

  private String[] buildWhitelist() {

    jwtAuthenticationFilter.addPathsToWhiteList(Arrays.asList(swaggerWhiteList));

    if (StringUtils.isEmpty(jwtAuthPathProperties.getWhitelist())) {
      return ArrayUtils.addAll(springSecurityWhiteList, swaggerWhiteList);
    }

    return Stream
        .of(
            ArrayUtils.addAll(springSecurityWhiteList, swaggerWhiteList),
            Stream
                .of(jwtAuthPathProperties.getWhitelist().split(","))
                .map(element -> element + "/**")
                .toArray(String[]::new))
        .flatMap(Stream::of)
        .toArray(String[]::new);
  }
}
