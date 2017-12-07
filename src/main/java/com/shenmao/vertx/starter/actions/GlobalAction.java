package com.shenmao.vertx.starter.actions;

import com.github.rjeschke.txtmark.Processor;
import com.shenmao.vertx.starter.MainVerticle;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.templ.FreeMarkerTemplateEngine;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class GlobalAction {

  private final FreeMarkerTemplateEngine templateEngine = FreeMarkerTemplateEngine.create();

  private static final Logger LOGGER = LoggerFactory.getLogger(MainVerticle.class);
  private static final String SQL_CREATE_PAGES_TABLE = "create table if not exists Pages (Id integer identity primary key, Name varchar(255) unique, Content clob)";
  private static final String SQL_GET_PAGE = "select Id, Name, Content from Pages where Id = ?";
  private static final String SQL_CREATE_PAGE = "insert into Pages values (NULL, ?, ?)";
  private static final String SQL_SAVE_PAGE = "update Pages set Content = ? where Id = ?";
  private static final String SQL_ALL_PAGES = "select Id, Name, Content from Pages";
  private static final String SQL_DELETE_PAGE = "delete from Pages where Id = ?";
  private static final String EMPTY_PAGE_MARKDOWN = "# A new page\n" +
    "\n" +
    "Feel-free to write in Markdown!\n";


  private JDBCClient dbClient;
  private Vertx _vertx;

  public GlobalAction(Vertx vertx) {

    _vertx = vertx;

    dbClient = JDBCClient.createShared(_vertx, new JsonObject()
      .put("url", "jdbc:hsqldb:file:db/wiki")
      .put("driver_class", "org.hsqldb.jdbcDriver")
      .put("max_pool_size", 30));

  }

  public Future<Void> prepareDatabase() {

    Future<Void> future = Future.future();


    dbClient.getConnection(ar -> {

      if (ar.failed() ) {
        LOGGER.error("Could not open a database connection##prepareDatabase", ar.cause());
        future.fail(ar.cause());
      } else {

        SQLConnection connection = ar.result();

        connection.execute(SQL_CREATE_PAGES_TABLE, create -> {
          connection.close();
          if (create.failed()) {
            LOGGER.error("Database preparation error", create.cause());
            future.fail(create.cause());
          } else {
            future.complete();
          }
        });

      }

    });

    return future;

  }

  public void staticHandler(RoutingContext context) {
    context.request().response().sendFile(context.request().path().substring(1));
  }

  public void indexHandler(RoutingContext context) {

    dbClient.getConnection(car -> {

      if (car.succeeded()) {

        SQLConnection connection = car.result();

        connection.query(SQL_ALL_PAGES, res -> {

          connection.close();

          if (res.succeeded()) {

            List<JsonArray> pages = res.result()
              .getResults()
//              .getRows();
              .stream()
              //.map(json -> json.getString(1))
//              .sorted()
              .collect(Collectors.toList());

//            pages.get(0).getInteger("id");
            context.put("title", "Wiki Home");
            context.put("pages", pages);
//            context.put("testjsonvalue", res.result().getRows().get(1));

            //System.out.println(((JsonObject)context.get("testjsonvalue")).getString("NAME"));


            templateEngine.render(context, "templates", "/index.ftl", ar -> {
              if (ar.succeeded()) {
                context.response().putHeader("Content-Type", "text/html");
                context.response().end(ar.result());
              } else {
                context.fail(ar.cause());
              }
            });

          } else {
            context.fail(res.cause());
          }
        });
      } else {
        context.fail(car.cause());
      }

    });

  }

  public void pageUpdateHandler(RoutingContext context) {

    String id = context.request().getParam("id");
    String title = context.request().getParam("title");
    String markdown = context.request().getParam("markdown");
    boolean newPage = "yes".equals(context.request().getParam("newPage"));

    dbClient.getConnection(car -> {

      if (car.succeeded()) {

        SQLConnection connection = car.result();
        String sql = newPage ? SQL_CREATE_PAGE : SQL_SAVE_PAGE;
        JsonArray params = new JsonArray();

        if (newPage) {
          params.add(title).add(markdown);
        } else {
          params.add(markdown).add(id);
        }

        connection.updateWithParams(sql, params, res -> {

          connection.close();

          if (res.succeeded()) {

            context.response().setStatusCode(303);
            context.response().putHeader("Location", "/wiki/" + id);
            context.response().end();

          } else {
            context.fail(res.cause());
          } });

      } else {
        context.fail(car.cause());
      }

    });

  }

  public void pageDeletionHandler(RoutingContext context) {

    String id = context.request().getParam("id");

    dbClient.getConnection(car -> {

      if (car.succeeded()) {

        SQLConnection connection = car.result();

        connection.updateWithParams(SQL_DELETE_PAGE, new JsonArray().add(id), res -> {

          connection.close();

          if (res.succeeded()) {

            context.response().setStatusCode(303);
            context.response().putHeader("Location", "/");
            context.response().end();

          } else {

            context.fail(res.cause());

          } });

      } else {
        context.fail(car.cause());
      }

    });

  }

  public void pageCreateHandler(RoutingContext context) {

    String page_title = context.request().getParam("name");
    String location = "/wiki/" + page_title;

//    pourData(page_title, null);

    if (page_title == null || page_title.isEmpty()) {
      location = "/wiki/1";
    } else location = "/wiki/2";

    location = "/";

    context.response().setStatusCode(303);
    context.response().putHeader("Location", location);
    context.response().end();

  }

  public void pageRenderingHandler(RoutingContext context) {

    final String _page_id = context.request().getParam("id");

    dbClient.getConnection(car -> {

      if (car.succeeded()) {

        SQLConnection connection = car.result();

        connection.queryWithParams(SQL_GET_PAGE, new JsonArray().add(_page_id), fetch -> {

          connection.close();

          if (fetch.succeeded()) {

            JsonArray row = fetch.result().getResults()
              .stream()
              .findFirst()
              .orElseGet(() -> new JsonArray().add(-1).add(EMPTY_PAGE_MARKDOWN));

            Integer id = row.getInteger(0);
            String title = row.getString(1);
            String rawContent = row.getString(2);

            context.put("id", id);
            context.put("title", title);
            context.put("newPage", fetch.result().getResults().size() == 0 ? "yes" : "no");
            context.put("rawContent", rawContent);
            context.put("content", Processor.process(rawContent));
            context.put("timestamp", new Date().toString());

            templateEngine.render(context, "templates", "/pages/page_detail.ftl", ar -> {

              if (ar.succeeded()) {
                context.response().putHeader("Content-Type", "text/html");
                context.response().end(ar.result());
              }
              else {
                context.fail(ar.cause());
              }

            });

          } else {
            context.fail(fetch.cause());
          }

        });

      } else {
        context.fail(car.cause());
      }

    });

  }

  private Future<Void> pourData(String title, String markdown) {

    Future<Void> future = Future.future();

    dbClient.getConnection(ar -> {

      if (ar.failed() ) {
        LOGGER.error("Could not open a database connection##pourData", ar.cause());
        future.fail(ar.cause());

      } else {

        SQLConnection connection = ar.result();

        JsonArray params = new JsonArray();

        if (title == null || title.trim().isEmpty()) {
          future.fail("Title could not be null!");
        } else {

          params.add("The title for four page from 张春晖");

          if (markdown != null)
            params.add("the markdown for four page from 张春晖");

          connection.updateWithParams(SQL_CREATE_PAGE, params, res -> {

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


}
