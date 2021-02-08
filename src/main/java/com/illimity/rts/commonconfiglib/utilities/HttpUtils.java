package com.illimity.rts.commonconfiglib.utilities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HttpUtils {

  private static final String ATTACHMENT = "attachment; filename=";

  private String url;
  private String gateway;
  private String uri;
  private String[] pathVariables;
  private MultiValueMap<String, String> queryParams;

  public static HttpHeaders populateDownloadHttpHeaders(String extension, String filename) {
    HttpHeaders responseHeaders = new HttpHeaders();

    responseHeaders.add(HttpHeaders.CONTENT_DISPOSITION,
        new StringBuilder()
            .append(ATTACHMENT)
            .append("\"")
            .append(filename)
            .append('.')
            .append(extension)
            .append("\"").toString());
    responseHeaders.add(HttpHeaders.CONTENT_TYPE, mediaType(extension));
    responseHeaders.setCacheControl(CacheControl.noCache());

    return responseHeaders;
  }


  public String createStringUrl() {
    if (pathVariables != null && pathVariables.length > 0) {
      return buildQueryParams(buildUrl())
          .buildAndExpand(pathVariables)
          .toUriString();
    }

    return buildQueryParams(buildUrl()).toUriString();
  }

  private UriComponentsBuilder buildUrl() {
    if (StringUtils.isEmpty(url)) {
      return UriComponentsBuilder.fromHttpUrl(gateway + uri);
    }

    return UriComponentsBuilder.fromHttpUrl(url);
  }

  private UriComponentsBuilder buildQueryParams(UriComponentsBuilder uriComponentsBuilder) {
    if (queryParams != null && queryParams.size() > 0) {
      return uriComponentsBuilder.queryParams(queryParams);
    }

    return uriComponentsBuilder;
  }

  private static String mediaType(String fileExtension) {

    switch (fileExtension.toLowerCase()) {
      case "pdf":
        return MediaType.APPLICATION_PDF_VALUE;
      case "xml":
      case "html":
        return MediaType.APPLICATION_XHTML_XML_VALUE;
      default:
        return MediaType.APPLICATION_OCTET_STREAM_VALUE;
    }
  }
}
