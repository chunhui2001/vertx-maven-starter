package com.shenmao.vertx.starter.database.wikipage;

import com.shenmao.vertx.starter.exceptions.PurposeException;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.rx.java.RxHelper;
import io.vertx.rxjava.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.ResultSet;
import io.vertx.rxjava.ext.sql.SQLConnection;
import rx.Single;

import java.util.ArrayList;
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

    this.createWikiPagesTable(voidAsyncResult -> {
      readyHandler.handle(Future.succeededFuture(this));
    });

//    dbClient.rxGetConnection().flatMap(sqlConnection -> {
//      Single<SQLConnection> connectionSingle = Single.just(sqlConnection);
//      return connectionSingle.doOnUnsubscribe(sqlConnection::close);
//    });

  }

  public void createWikiPagesTable(Handler<AsyncResult<Void>> resultHandler) {

    dbClient.getConnection(ar -> {
      if (ar.failed()) {
        LOGGER.error("Could not open a database connection", ar.cause());
        resultHandler.handle(Future.failedFuture(ar.cause()));
      } else {
        SQLConnection connection = ar.result();
        connection.execute(sqlQueriesWikiPages.get(WikiSqlQuery.CREATE_WIKI_PAGES_TABLE), create -> {
          connection.close();
          if (create.succeeded())
            resultHandler.handle(Future.succeededFuture());
          else {
            LOGGER.error("Database preparation error", Future.failedFuture(create.cause()));
            resultHandler.handle(Future.failedFuture(create.cause()));
          }
        });
      }
    });
  }


  @Override
  public WikiPageDbService fetchAllPages(Handler<AsyncResult<List<JsonObject>>> resultHandler) {

    dbClient.rxQuery(sqlQueriesWikiPages.get(WikiSqlQuery.ALL_WIKI_PAGES))
      .map(a -> {

        // a.getRows().forEach();

        JsonObject p1 = new JsonObject()
          .put("wiki_page_title", "firt wiki page")
          .put("wiki_page_id", 1)
          .put("markdown", "markdown 1")
          .put("page_content", "page_content 1");

        JsonObject p2 = new JsonObject()
          .put("wiki_page_title", "second wiki page")
          .put("wiki_page_id", 2)
          .put("markdown", "markdown 1")
          .put("page_content", "page_content 2");

        JsonObject p3 = new JsonObject()
          .put("wiki_page_title", "dbObject.getString(\"name\")")
          .put("wiki_page_id", "dbObject.getInteger(\"id\")")
          .put("markdown", "dbObject.getString(\"content\")")
          .put("page_content", "dbObject.getString(\"content\"))");

        List<JsonObject> list = new ArrayList<>();

        list.add(p1);
        list.add(p2);
        list.add(p3);
        list.stream().sorted();
        return list;
      })
      .subscribe(RxHelper.toSubscriber(resultHandler));

    return this;
  }

  @Override
  public WikiPageDbService fetchPage(String name, Handler<AsyncResult<JsonObject>> resultHandler) {


    dbClient.rxQueryWithParams(sqlQueriesWikiPages.get(WikiSqlQuery.GET_WIKI_PAGE), new JsonArray().add(name))
      .map(a -> {
        JsonObject response = new JsonObject();
        List<JsonArray> resultSet = a.getResults();

        if (resultSet.size() == 0) {
          response.put("found", false);
        } else {
          response.put("found", true);
          JsonArray row = resultSet.get(0);
          response.put("wiki_page_id", row.getInteger(0));
          response.put("page_content", row.getString(1));
        }

        return response;
      }).subscribe(RxHelper.toSubscriber(resultHandler));


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
