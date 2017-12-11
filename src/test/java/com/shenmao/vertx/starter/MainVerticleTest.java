package com.shenmao.vertx.starter;

import com.shenmao.vertx.starter.commons.HttpGets;
import com.shenmao.vertx.starter.commons.HttpPosts;
import com.shenmao.vertx.starter.commons.HttpResult;
import com.shenmao.vertx.starter.configuration.ApplicationConfig;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

import org.apache.commons.io.FileUtils;
import org.apache.http.message.BasicNameValuePair;
import org.junit.*;
import org.junit.runner.RunWith;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

@RunWith(VertxUnitRunner.class)
public class MainVerticleTest {

  public static int _PORT = Integer.parseInt(Application.getAppConfig().get(ApplicationConfig.AppConfig.APP_PORT));
  public static String _HOST = Application.getAppConfig().get(ApplicationConfig.AppConfig.APP_HOST);
  public static String _URL = Application.applicationConfigInstance().getUrl(false);

  private Vertx vertx;
  private HttpClient _httpClient;


  @Before
  public void setUp(TestContext tc) {
    vertx = Vertx.vertx();
    vertx.deployVerticle(MainVerticle.class.getName(), tc.asyncAssertSuccess());
    _httpClient = vertx.createHttpClient();
  }

  @After
  public void tearDown(TestContext tc) throws IOException {

    Path hsqldb_folder = Paths.get("db");

    if (Files.exists(hsqldb_folder)) {
      FileUtils.forceDelete(hsqldb_folder.toFile());
    }

    vertx.close(tc.asyncAssertSuccess());
  }

  @Test
  public void testThatTheServerIsStarted(TestContext tc) {

    Async async = tc.async();

    _httpClient.getNow(_PORT, _HOST, "/", response -> {
      tc.assertEquals(response.statusCode(), 200);
      response.bodyHandler(body -> {
        tc.assertTrue(body.length() > 0);
        async.complete();
      });
    });
  }

  private Future<Map<String, Object>> createPage(BasicNameValuePair params) {

    Future<Map<String, Object>> future = Future.future();
    Map<String, Object> result = new HashMap<>();

    HttpResult httpResult = HttpPosts.execute(_URL + "/create", params);
    result.put("statusCode", httpResult.getStatusCode());
    result.put("newPageId", httpResult.getContent());

    future.complete(result);

    return future;

  }

  @Test
  public void testTheCreatePageIsFun(TestContext tc) {

    Async async = tc.async();

    Future<Map<String, Object>>
      steps = createPage(new BasicNameValuePair("name", "www"));
    //.compose(v -> createPage(new BasicNameValuePair("name", "www2")));

    steps.setHandler(ar -> {

      tc.assertTrue(ar.succeeded(), "testTheCreatePageIsFun##创建页面异常: " + ar.cause());

      int statusCode = (int) ar.result().get("statusCode");
      String newPageId = ar.result().get("newPageId").toString();

      tc.assertEquals(statusCode, 301, "创建成功后需返回 301 状态, 页面跳转 ");
      tc.assertFalse(newPageId.isEmpty(), "创建Page成功后返回新的 ID: [" + newPageId + "]");

      async.complete();

    });

  }


  @Test
  public void testTheUpdatePageIsFun(TestContext tc) {

    Async async = tc.async();

    HttpResult httpResult = HttpPosts.execute(_URL + "/save",
              new BasicNameValuePair("id", "0"),
              new BasicNameValuePair("markdown", "markdown updated"),
              new BasicNameValuePair("title", "new title"))
      ;

    String updatedPageId = httpResult.getContent();


    tc.assertEquals(httpResult.getStatusCode(), 301, "更新成功后需返回 301 状态, 页面跳转 ");
    tc.assertEquals(updatedPageId, updatedPageId, "已经更新的 Page ID: [" + updatedPageId + "]");

    async.complete();

  }

  @Test
  public void testThePageListIsFun(TestContext tc) {

    Async async = tc.async();

    HttpResult httpResult = HttpGets.execute(_URL + "/");
    tc.assertEquals(httpResult.getStatusCode(), 200, "列表不为空返回 200 ");
    //tc.assertEquals(httpResult.getStatusCode(), 204, "列表为空返回 204 ");

    async.complete();

  }

  @Test
  public void testTheDetailPageIsFun(TestContext tc) {

    Async async = tc.async();

    HttpResult httpResult = HttpGets.execute(_URL + "/wiki/1");
    tc.assertEquals(httpResult.getStatusCode(), 200, "成功找到一个 Page 返回 200 ");
    //tc.assertEquals(httpResult.getStatusCode(), 404, "访问不存在的 Pge 返回 404 ");

    async.complete();

  }

  @Test
  public void testDeletePageIsFun(TestContext tc) {

    Async async = tc.async();
    String pageIdTobeDel = "1";

    HttpResult httpResult = HttpPosts.execute(_URL + "/delete", new BasicNameValuePair("id", pageIdTobeDel));
    String deletedPageId = httpResult.getContent();

    tc.assertEquals(deletedPageId, pageIdTobeDel, "已经删除的 Page ID: [" + deletedPageId + "]");
    tc.assertEquals(httpResult.getStatusCode(), 301, "删除成功后需返回 301 状态, 页面跳转 ");

    async.complete();


  }


}



