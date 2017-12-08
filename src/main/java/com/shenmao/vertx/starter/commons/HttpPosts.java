package com.shenmao.vertx.starter.commons;

import org.apache.http.*;
import org.apache.http.HttpResponse;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class HttpPosts {

  public static HttpResult execute(String url, BasicNameValuePair... params) {

    HttpResult result = new HttpResult() ;

    try (CloseableHttpClient httpClient = HttpClientBuilder.create().build()) {

      List<NameValuePair> urlParameters = new ArrayList<>();

      for (BasicNameValuePair p : params) {
        urlParameters.add(p);
      }


      org.apache.http.client.methods.HttpPost httpPost = new org.apache.http.client.methods.HttpPost(url);

      httpPost.setEntity(new UrlEncodedFormEntity(urlParameters));
      HttpResponse httpResponse = httpClient.execute(httpPost);

      BufferedReader rd = new BufferedReader(
        new InputStreamReader(httpResponse.getEntity().getContent()));

      StringBuffer buffer = new StringBuffer();

      String line = "";

      while ((line = rd.readLine()) != null) {
        buffer.append(line);
      }

      result.setStatusCode(httpResponse.getStatusLine().getStatusCode());
      result.setContent(buffer.toString());

    } catch (IOException e) {
      result.setError(true);
      result.setMessage(e.getCause().toString());
    }

    return result;

  }


}
