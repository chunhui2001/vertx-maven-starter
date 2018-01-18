package com.shenmao.vertx.starter.database.wikipage;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.SQLConnection;

import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class WikiPageDbServiceImpl implements WikiPageDbService {

  private static final Logger LOGGER = LoggerFactory.getLogger(WikiPageDbServiceImpl.class);
  private final HashMap<WikiSqlQuery, String> sqlQueriesWikiPages;
  private final JDBCClient dbClient;

  WikiPageDbServiceImpl(JDBCClient dbClient, HashMap<WikiSqlQuery, String> sqlQueries,
                        Handler<AsyncResult<WikiPageDbService>> readyHandler) {

    this.dbClient = dbClient;
    this.sqlQueriesWikiPages = sqlQueries;

    dbClient.getConnection(ar -> {
      if (ar.failed()) {
        LOGGER.error("Could not open a database connection", ar.cause());
        readyHandler.handle(Future.failedFuture(ar.cause()));
      } else {
        SQLConnection connection = ar.result();
        connection.execute(sqlQueries.get(WikiSqlQuery.CREATE_WIKI_PAGES_TABLE), create -> {
          connection.close();
          if (create.failed()) {
            LOGGER.error("Database preparation error", create.cause());
            readyHandler.handle(Future.failedFuture(create.cause()));
          } else {
            readyHandler.handle(Future.succeededFuture(this));
          }
        });
      }
    });
  }


  @Override
  public WikiPageDbService fetchAllPages(Handler<AsyncResult<JsonArray>> resultHandler) {

    dbClient.query(sqlQueriesWikiPages.get(WikiSqlQuery.ALL_WIKI_PAGES), res -> {

      if (res.succeeded()) {
        JsonArray pages = new JsonArray(res.result()
          .getResults()
          .stream()
          .map(json -> json.getString(0))
          .sorted()
          .collect(Collectors.toList()));

        resultHandler.handle(Future.succeededFuture(pages));
      } else {
        LOGGER.error("Database query error", res.cause());
        resultHandler.handle(Future.failedFuture(res.cause()));
      }
    });

    return this;
  }

  @Override
  public WikiPageDbService fetchPage(String name, Handler<AsyncResult<JsonObject>> resultHandler) {

    dbClient.queryWithParams(

      sqlQueriesWikiPages.get(WikiSqlQuery.GET_WIKI_PAGE), new JsonArray().add(name), fetch -> {

      if (fetch.succeeded()) {

        JsonObject response = new JsonObject();
        ResultSet resultSet = fetch.result();

        if (resultSet.getNumRows() == 0) {
          response.put("found", false);
        } else {
          response.put("found", true);
          JsonArray row = resultSet.getResults().get(0);
          response.put("wiki_page_id", row.getInteger(0));
          response.put("page_content", row.getString(1));
        }

        resultHandler.handle(Future.succeededFuture(response));
      } else {
        LOGGER.error("Database query error", fetch.cause());
        resultHandler.handle(Future.failedFuture(fetch.cause()));
      }

    });

    return this;
  }

  @Override
  public WikiPageDbService createPage(String title, String markdown, Handler<AsyncResult<Void>> resultHandler) {

    JsonArray data = new JsonArray().add(title).add(markdown);

    dbClient.updateWithParams(sqlQueriesWikiPages.get(WikiSqlQuery.CREATE_WIKI_PAGE), data, res -> {
      if (res.succeeded()) {
        resultHandler.handle(Future.succeededFuture());
      } else {
        LOGGER.error("Database query error", res.cause());
        resultHandler.handle(Future.failedFuture(res.cause()));
      }
    });

    return this;
  }

  @Override
  public WikiPageDbService savePage(int id, String markdown, Handler<AsyncResult<Void>> resultHandler) {

    JsonArray data = new JsonArray().add(markdown).add(id);

    dbClient.updateWithParams(sqlQueriesWikiPages.get(WikiSqlQuery.SAVE_WIKI_PAGE), data, res -> {
      if (res.succeeded()) {
        resultHandler.handle(Future.succeededFuture());
      } else {
        LOGGER.error("Database query error", res.cause());
        resultHandler.handle(Future.failedFuture(res.cause()));
      }
    });

    return this;
  }

  @Override
  public WikiPageDbService deletePage(int id, Handler<AsyncResult<Void>> resultHandler) {

    JsonArray data = new JsonArray().add(id);

    dbClient.updateWithParams(sqlQueriesWikiPages.get(WikiSqlQuery.DELETE_WIKI_PAGE), data, res -> {
      if (res.succeeded()) {
        resultHandler.handle(Future.succeededFuture());
      } else {
        LOGGER.error("Database query error", res.cause());
        resultHandler.handle(Future.failedFuture(res.cause()));
      }
    });

    return this;
  }
}
