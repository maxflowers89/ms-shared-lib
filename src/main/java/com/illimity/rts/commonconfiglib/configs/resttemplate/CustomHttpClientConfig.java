package com.illimity.rts.commonconfiglib.configs.resttemplate;

import org.apache.http.HeaderElement;
import org.apache.http.HeaderElementIterator;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicHeaderElementIterator;
import org.apache.http.protocol.HTTP;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CustomHttpClientConfig {

  @Value("${rest-template.http.millis-connection-timeout:30000}")
  private int connectionTimeout;

  @Value("${rest-template.http.millis-socket-timeout:30000}")
  private int socketTimout;

  @Value("${rest-template.http.millis-request-timeout:30000}")
  private int requestTimeout;

  @Value("${rest-template.http.max-total-connections:10}")
  private int maxTotalConnections;

  @Value("${rest-template.http.default-max-per-route:5}")
  private int defaultMaxPerRoute;

  @Value("${rest-template.http.millis-keep-alive:2000}")
  private int keepAliveTime;

  @Bean
  protected CloseableHttpClient httpClient() {
    RequestConfig requestConfig = RequestConfig.custom()
        .setConnectionRequestTimeout(requestTimeout)
        .setConnectTimeout(connectionTimeout)
        .setSocketTimeout(socketTimout)
        .build();

    return HttpClients.custom()
        .setDefaultRequestConfig(requestConfig)
        .setConnectionManager(poolingConnectionManager())
        .setKeepAliveStrategy(connectionKeepAliveStrategy())
        .build();
  }

  private PoolingHttpClientConnectionManager poolingConnectionManager() {
    PoolingHttpClientConnectionManager poolingConnectionManager = new PoolingHttpClientConnectionManager();
    poolingConnectionManager.setMaxTotal(maxTotalConnections);
    poolingConnectionManager.setDefaultMaxPerRoute(defaultMaxPerRoute);

    return poolingConnectionManager;
  }

  private ConnectionKeepAliveStrategy connectionKeepAliveStrategy() {
    return (response, context) -> {
      HeaderElementIterator it = new BasicHeaderElementIterator
          (response.headerIterator(HTTP.CONN_KEEP_ALIVE));
      while (it.hasNext()) {
        HeaderElement he = it.nextElement();
        String param = he.getName();
        String value = he.getValue();

        if (value != null && param.equalsIgnoreCase("timeout")) {
          return Long.parseLong(value) * 1000;
        }
      }
      return keepAliveTime;
    };
  }
}
