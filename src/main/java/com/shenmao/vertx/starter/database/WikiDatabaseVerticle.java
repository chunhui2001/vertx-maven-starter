package com.shenmao.vertx.starter.database;

import com.shenmao.vertx.starter.Application;
import com.shenmao.vertx.starter.configuration.SqlQueriesConfig;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.serviceproxy.ProxyHelper;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;

public class WikiDatabaseVerticle extends AbstractVerticle {

  private HashMap<SqlQueriesConfig.SqlQuery, String> sqlQueries = Application.getSqlQueriesConfig();
  private static final Logger LOGGER = LoggerFactory.getLogger(WikiDatabaseVerticle.class);

  public static final String CONFIG_WIKIDB_QUEUE = "wikidb.queue";

  public static final String EMPTY_PAGE_MARKDOWN = "# A new page\n" +
    "\n" +
    "Feel-free to write in Markdown!\n";

  private JDBCClient _dbClient;


  private Future<Void> pourData(String title, String markdown) {

    Future<Void> future = Future.future();

    _dbClient.getConnection(ar -> {

      if (ar.failed() ) {
        LOGGER.error("Could not open a database connection##pourData", ar.cause());
        future.fail(ar.cause());

      } else {

        SQLConnection connection = ar.result();

        JsonArray params = new JsonArray();

        if (title == null || title.trim().isEmpty()) {
          future.fail("Title could not be null!");
        } else {

          params.add(title);

          if (markdown == null)
            params.add(EMPTY_PAGE_MARKDOWN);

          connection.updateWithParams(sqlQueries.get(SqlQueriesConfig.SqlQuery.CREATE_PAGE), params, res -> {

            connection.close();
            if (res.failed()) {
              LOGGER.error("Database preparation error##pourData", res.cause());
              future.fail(res.cause());
            } else {
              future.complete();
            }

          });
        }

      }

    });

    return future;

  }

  private Future<Void> dropTable(String tableName) {

    Future<Void> future = Future.future();

    _dbClient.query(sqlQueries.get(SqlQueriesConfig.SqlQuery.DROP_TABLE) + " " + tableName, drop -> {
      if (drop.failed()) {
        LOGGER.error("Drop table error##WikiDatabaseVerticle.prepareDatabase", drop.cause());
        future.fail(drop.cause());
      } else {
        future.complete();
      }
    });

    return future;

  }

  private Future<Void> createTable() {

    Future<Void> future = Future.future();

    _dbClient.query(sqlQueries.get(SqlQueriesConfig.SqlQuery.CREATE_PAGES_TABLE), create -> {
      if (create.failed()) {
        LOGGER.error("Database preparation error##WikiDatabaseVerticle.prepareDatabase", create.cause());
        future.fail(create.cause());
      } else {
        future.complete();
      }
    });

    return future;

  }




  @Override
  public void start(Future<Void> startFuture) throws IOException {


    _dbClient = JDBCClient.createShared(vertx, new JsonObject()
      .put("url", "jdbc:hsqldb:file:db/wiki")
      .put("driver_class", "org.hsqldb.jdbcDriver")
      .put("max_pool_size", 30));

    WikiDatabaseService.create(_dbClient, sqlQueries, ready -> {

      if (ready.succeeded()) {

        ProxyHelper.registerService(WikiDatabaseService.class, vertx, ready.result(), CONFIG_WIKIDB_QUEUE);

        Future<Void> steps = //dropTable("Pages").compose(v -> createTable())

        createTable()
            .compose(v -> pourData("The title for first page.", null))
            .compose(v -> pourData("The title for second page.", null))
          .compose(v -> pourData("The title for three page.", null))
          .compose(v -> pourData("The title for four page.", null))
           .compose(v -> pourData("The title for five page.", null))
          ;

        steps.setHandler(ar -> {
          if (ar.succeeded()) {
//            vertx.eventBus().consumer(CONFIG_WIKIDB_QUEUE, this::onMessage);
            startFuture.complete();
          } else {
            startFuture.fail(ar.cause());
          }
        });



      } else {
        startFuture.fail(ready.cause());
      }


    });




  }


}
