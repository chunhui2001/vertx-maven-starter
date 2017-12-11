package com.shenmao.vertx.starter.database;

import com.shenmao.vertx.starter.configuration.SqlQueriesConfig;
import io.vertx.codegen.annotations.Fluent;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;

import java.util.HashMap;
import java.util.List;

@ProxyGen
public interface WikiDatabaseService {

  static WikiDatabaseService create(JDBCClient jdbcClient, HashMap<SqlQueriesConfig.SqlQuery, String> sqlQueries, Handler<AsyncResult<WikiDatabaseService>> resultHandler) {
    return new WikiDatabaseServiceImpl(jdbcClient, sqlQueries, resultHandler);
  }

  static WikiDatabaseService createProxy(Vertx vertx, String address) {
    return new WikiDatabaseServiceVertxEBProxy(vertx, address);
  }


    @Fluent
  WikiDatabaseService fetchAllPages(Handler<AsyncResult<List<JsonArray>>> resultHandler);

  @Fluent
  WikiDatabaseService fetchPage(Long id, Handler<AsyncResult<JsonObject>> resultHandler);

  @Fluent
  WikiDatabaseService createPage(String title, String markdown, Handler<AsyncResult<Long>> resultHandler);

  @Fluent
  WikiDatabaseService savePage(Long id, String title, String markdown, Handler<AsyncResult<Void>> resultHandler);

  @Fluent
  WikiDatabaseService deletePage(Long id, Handler<AsyncResult<Void>> resultHandler);

  @Fluent
  WikiDatabaseService fetchLastIncrementId(Handler<AsyncResult<Long>> resultHandler);

}
