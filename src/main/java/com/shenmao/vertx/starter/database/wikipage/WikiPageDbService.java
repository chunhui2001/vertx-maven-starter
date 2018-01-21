package com.shenmao.vertx.starter.database.wikipage;

import io.vertx.codegen.annotations.Fluent;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.ext.jdbc.JDBCClient;
import io.vertx.core.Vertx;


import java.util.HashMap;
import java.util.List;

@ProxyGen
public interface WikiPageDbService {

  public static WikiPageDbService create(JDBCClient dbClient, HashMap<WikiSqlQuery, String> sqlQueries,
                                         Handler<AsyncResult<WikiPageDbService>> readyHandler) {
    return new WikiPageDbServiceImpl(dbClient, sqlQueries, readyHandler);
  }

  public static WikiPageDbService createProxy(Vertx vertx, String address) {
    return new WikiPageDbServiceVertxEBProxy(vertx, address);
  }

  @Fluent
  WikiPageDbService fetchAllPages(Handler<AsyncResult<List<JsonObject>>> resultHandler);

  @Fluent
  WikiPageDbService fetchPage(String name, Handler<AsyncResult<JsonObject>> resultHandler);

  @Fluent
  WikiPageDbService createPage(String title, String markdown, Handler<AsyncResult<Void>> resultHandler);

  @Fluent
  WikiPageDbService savePage(int id, String markdown, Handler<AsyncResult<Void>> resultHandler);

  @Fluent
  WikiPageDbService deletePage(int id, Handler<AsyncResult<Void>> resultHandler);

}
