package com.illimity.rts.commonconfiglib.callsservices;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

public interface CallsService {

  default HttpHeaders setAuth(String auth) {
    HttpHeaders httpHeaders = new HttpHeaders();

    httpHeaders.setContentType(MediaType.APPLICATION_JSON);
    httpHeaders.set(HttpHeaders.AUTHORIZATION, auth);

    return httpHeaders;
  }
}
