package com.shenmao.vertx.starter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

public class WikiDatabaseVerticle extends AbstractVerticle {

  public enum ErrorCodes {
    NO_ACTION_SPECIFIED,
    BAD_ACTION,
    DB_ERROR
  }

  public enum SqlQuery {
    DROP_TABLE,
    CREATE_PAGES_TABLE,
    ALL_PAGES,
    GET_PAGE,
    CREATE_PAGE,
    SAVE_PAGE,
    DELETE_PAGE,
    LAST_INSERT_ID
  }

  private final HashMap<SqlQuery, String> sqlQueries = new HashMap<>();
  private static final Logger LOGGER = LoggerFactory.getLogger(WikiDatabaseVerticle.class);

  public static final String CONFIG_WIKIDB_JDBC_URL = "wikidb.jdbc.url";
  public static final String CONFIG_WIKIDB_JDBC_DRIVER_CLASS = "wikidb.jdbc.driver_class";
  public static final String CONFIG_WIKIDB_JDBC_MAX_POOL_SIZE = "wikidb.jdbc.max_pool_size";
  public static final String CONFIG_WIKIDB_SQL_QUERIES_RESOURCE_FILE = "wikidb.sqlqueries.resource.file";
  public static final String CONFIG_WIKIDB_QUEUE = "wikidb.queue";

  public static final String EMPTY_PAGE_MARKDOWN = "# A new page\n" +
    "\n" +
    "Feel-free to write in Markdown!\n";

  private JDBCClient _dbClient;

  private void loadSqlQueries() {

    String queriesFile = config().getString(CONFIG_WIKIDB_SQL_QUERIES_RESOURCE_FILE);

    InputStream queriesInputStream = null;

    Properties queriesProps = new Properties();

    try {

      if (queriesFile == null) {
        queriesInputStream = getClass().getResourceAsStream("/properties/db-queries.properties");
      } else {

          queriesInputStream = new FileInputStream(queriesFile);

      }

      queriesProps.load(queriesInputStream);
      queriesInputStream.close();
    } catch (Exception e) {
      e.printStackTrace();
    }

    sqlQueries.put(SqlQuery.DROP_TABLE, queriesProps.getProperty("drop-table"));
    sqlQueries.put(SqlQuery.CREATE_PAGES_TABLE, queriesProps.getProperty("create-pages-table"));
    sqlQueries.put(SqlQuery.ALL_PAGES, queriesProps.getProperty("all-pages"));
    sqlQueries.put(SqlQuery.GET_PAGE, queriesProps.getProperty("get-page"));
    sqlQueries.put(SqlQuery.CREATE_PAGE, queriesProps.getProperty("create-page"));
    sqlQueries.put(SqlQuery.SAVE_PAGE, queriesProps.getProperty("save-page"));
    sqlQueries.put(SqlQuery.DELETE_PAGE, queriesProps.getProperty("delete-page"));
    sqlQueries.put(SqlQuery.LAST_INSERT_ID, queriesProps.getProperty("get-last-increment-id"));

  }

  public void fetchAllPages(Message<JsonObject> message) {

    _dbClient.query(sqlQueries.get(SqlQuery.ALL_PAGES), res -> {

      if (res.succeeded()) {

//        List<String> pages = res.result()
//          .getResults()
//          .stream()
//          .map(json -> json.getString(0))
//          .sorted()
//          .collect(Collectors.toList());


        message.reply(new JsonObject().put("pages", res.result().getResults()));

      } else {
        reportQueryError(message, res.cause());
      }

    });
  }

  public void fetchPage(Message<JsonObject> message) {

    final String _page_id = message.body().getString("id");
    JsonArray params = new JsonArray().add(_page_id);

    _dbClient.queryWithParams(sqlQueries.get(SqlQuery.GET_PAGE), params, fetch -> {

      if (fetch.succeeded()) {

        JsonObject response = new JsonObject();
        ResultSet resultSet = fetch.result();

        if (resultSet.getNumRows() == 0) {
          response.put("found", false);
        } else {

          response.put("found", true);

          JsonArray row = resultSet.getResults().get(0);

          response.put("id", row.getInteger(0));
          response.put("title", row.getString(1));
          response.put("rawContent", row.getString(2));
        }

        message.reply(response);

      } else {
        reportQueryError(message, fetch.cause());
      }

    });

  }

  public void createPage(Message<JsonObject> message) {

    JsonObject request = message.body();

    JsonArray _data = new JsonArray()
      .add(request.getString("title"))
      .add(request.getString("markdown"));

    _dbClient.updateWithParams(sqlQueries.get(SqlQuery.CREATE_PAGE), _data, res -> {

      if (res.succeeded()) {

        fetchLastIncrementId(reply -> {

          List<JsonArray> result = reply;

          if (result != null) {
            message.reply((long)result.get(0).getValue(0));
          } else {
            message.reply(null);
          }


        });

      } else {
        reportQueryError(message, res.cause());
      }

    });

  }

  public void fetchLastIncrementId(Handler<List<JsonArray>> done) {

    Future<List<JsonArray>> future = Future.future();

    _dbClient.query(sqlQueries.get(SqlQuery.LAST_INSERT_ID), res -> {

      if (res.succeeded()) {
        future.complete(res.result().getResults());
        done.handle(res.result().getResults());
      } else {
        done.handle(null);
      }
    });

  }


  public void savePage(Message<JsonObject> message) {

    JsonObject request = message.body();

    JsonArray data = new JsonArray()
      .add(request.getString("markdown"))
      .add(request.getString("id"));

    _dbClient.updateWithParams(sqlQueries.get(SqlQuery.SAVE_PAGE), data, res -> {

      if (res.succeeded()) {
        message.reply("ok");
      } else {
        reportQueryError(message, res.cause());
      }

    });

  }

  public void deletePage(Message<JsonObject> message) {
    JsonArray data = new JsonArray().add(message.body().getString("id"));

    _dbClient.updateWithParams(sqlQueries.get(SqlQuery.DELETE_PAGE), data, res -> {
      if (res.succeeded()) {
        message.reply("ok");
      } else {
        reportQueryError(message, res.cause());
      }
    });
  }

  private void reportQueryError(Message<JsonObject> message, Throwable cause) {
    LOGGER.error("Database query error!", cause);
    message.fail(ErrorCodes.DB_ERROR.ordinal(), cause.getMessage());
  }


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

          connection.updateWithParams(sqlQueries.get(SqlQuery.CREATE_PAGE), params, res -> {

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

    _dbClient.query(sqlQueries.get(SqlQuery.DROP_TABLE) + " " + tableName, drop -> {
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

    _dbClient.query(sqlQueries.get(SqlQuery.CREATE_PAGES_TABLE), create -> {
      if (create.failed()) {
        LOGGER.error("Database preparation error##WikiDatabaseVerticle.prepareDatabase", create.cause());
        future.fail(create.cause());
      } else {
        future.complete();
      }
    });

    return future;

  }

  public void onMessage(Message<JsonObject> message) {

    if (!message.headers().contains("action")) {
      LOGGER.error("No action header specified for message with headers {} and body {}", message.headers(), message.body().encodePrettily());
      message.fail(ErrorCodes.NO_ACTION_SPECIFIED.ordinal(), "No action header specified");
      return;
    }

    String action = message.headers().get("action");

    switch (action) {

      case "all-pages":
        fetchAllPages(message);
        break;

      case "get-page":
       fetchPage(message);
        break;

      case "create-page":
        createPage(message);
        break;

      case "save-page":
        savePage(message);
        break;

      case "delete-page":
        deletePage(message);
        break;

      default:
        message.fail(ErrorCodes.BAD_ACTION.ordinal(), "Bad action: " + action);

    }

  }




  @Override
  public void start(Future<Void> startFuture) throws IOException {


    loadSqlQueries();

    _dbClient = JDBCClient.createShared(vertx, new JsonObject()
      .put("url", config().getString(CONFIG_WIKIDB_JDBC_URL, "jdbc:hsqldb:file:db/wiki"))
      .put("driver_class", config().getString(CONFIG_WIKIDB_JDBC_DRIVER_CLASS, "org.hsqldb.jdbcDriver"))
      .put("max_pool_size", config().getInteger(CONFIG_WIKIDB_JDBC_MAX_POOL_SIZE, 30)));


    Future<Void> steps = dropTable("Pages").compose(v -> createTable())
      .compose(v ->pourData("The title for first page.", null))
      .compose(v -> pourData("The title for second page.", null))
      .compose(v -> pourData("The title for three page.", null))
      .compose(v -> pourData("The title for four page.", null))
      .compose(v -> pourData("The title for five page.", null))
      ;

    steps.setHandler(ar -> {
      if (ar.succeeded()) {
        vertx.eventBus().consumer("wikidb.queue", this::onMessage);
        startFuture.complete();
      } else {
        startFuture.fail(ar.cause());
      }
    });

  }


}
