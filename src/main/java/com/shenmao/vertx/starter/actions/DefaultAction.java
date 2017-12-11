package com.shenmao.vertx.starter.actions;

import com.github.rjeschke.txtmark.Processor;
import com.shenmao.vertx.starter.database.WikiDatabaseService;
import com.shenmao.vertx.starter.database.WikiDatabaseVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.templ.FreeMarkerTemplateEngine;

import java.util.Date;

import static com.shenmao.vertx.starter.database.WikiDatabaseVerticle.EMPTY_PAGE_MARKDOWN;

public class DefaultAction implements Action {

  private static final Logger LOGGER = LoggerFactory.getLogger(DefaultAction.class);
  private final FreeMarkerTemplateEngine templateEngine = FreeMarkerTemplateEngine.create();
  private String wikiDbQueue = WikiDatabaseVerticle.CONFIG_WIKIDB_QUEUE;


  private WikiDatabaseService dbService;

  private Vertx _vertx;

  public DefaultAction(Vertx vertx) {
    _vertx = vertx;
    dbService = WikiDatabaseService.createProxy(vertx, wikiDbQueue);
  }

  @Override
  public void indexHandler(RoutingContext context) {

    dbService.fetchAllPages(reply -> {

      if (reply.succeeded()) {

        context.put("title", "Wiki Home 2");
        context.put("pages", reply.result());

        templateEngine.render(context, "templates", "/index.ftl", ar -> {

          if (ar.succeeded()) {
            context.response().putHeader("Content-Type", "text/html");
            context.response().end(ar.result());
          } else {
            context.fail(ar.cause());
          }

        });

      } else {
        context.fail(reply.cause());
      }

    });


  }

  @Override
  public void pageUpdateHandler(RoutingContext context) {

    String _page_id = context.request().getParam("id");
    String _new_page = context.request().getParam("newPage");

      JsonObject _params = new JsonObject()
      .put("id", _page_id == null || _page_id.isEmpty() ? -1 : _page_id)
      .put("title", context.request().getParam("title"))
      .put("markdown", context.request().getParam("markdown"));


    Handler<AsyncResult<Long>> createHandler = reply -> {

      if (reply.succeeded()) {

        if (reply.result() == null || reply.result() == -1L) {
          context.response().setStatusCode(400);
          // TODO should be going to error page
          //context.response().putHeader("Location", "/wiki/" + reply.result());
        } else {
          context.response().setStatusCode(301);
          context.response().putHeader("Location", "/wiki/" + reply.result());
        }

        context.response().end();

      } else {
        context.fail(reply.cause());
      }

    };

    Handler<AsyncResult<Void>> updateHandler = reply -> {

      if (reply.succeeded()) {

        context.response().setStatusCode(301);
        context.response().putHeader("Location", "/wiki/" + _page_id);

        context.response().end();

      } else {
        context.fail(reply.cause());
      }

    };


    if ("yes".equals(_new_page)) {
      dbService.createPage(context.request().getParam("title"), context.request().getParam("markdown"), createHandler);
    } else {
      dbService.savePage(Long.parseLong(_page_id), context.request().getParam("title"), context.request().getParam("markdown"), updateHandler);
    }


  }

  @Override
  public void pageDeletionHandler(RoutingContext context) {

    Long id = Long.parseLong(context.request().getParam("id"));
    JsonObject request = new JsonObject().put("id", id);
    DeliveryOptions options = new DeliveryOptions().addHeader("action", "delete-page");

    dbService.deletePage(id, reply -> {

      if (reply.succeeded()) {
        context.response().setStatusCode( 301 );
        context.response().putHeader("Location", "/");
        context.response().end(id + "");
      } else {
        context.fail(reply.cause());
      }

    });

  }

  @Override
  public void pageCreateHandler(RoutingContext context) {

    String pageName = context.request().getParam("name");

    if (pageName == null || pageName.isEmpty()) {
      context.response().setStatusCode(400);
      context.response().putHeader("Location", "/");
      context.response().end();
    } else {

      dbService.createPage(pageName, EMPTY_PAGE_MARKDOWN, reply -> {

        Long newPageId = -1L;

        if (reply.succeeded()) {

          if (reply.result() == null) {
            context.response().putHeader("Location", "/");
          } else {
            newPageId = (long) reply.result();
            context.response().putHeader("Location", "/wiki/" + newPageId);
          }

          context.response().setStatusCode(301);

          context.response().end(newPageId + "");

        } else {
          context.fail(reply.cause());
        }

      });

    }

  }

  @Override
  public void pageRenderingHandler(RoutingContext context) {

    long _page_id = Long.parseLong(context.request().getParam("id"));

    dbService.fetchPage(_page_id, reply -> {

      if (reply.succeeded()) {

        JsonObject body = reply.result();

        boolean found = body.getBoolean("found");
        String rawContent = body.getString("rawContent", EMPTY_PAGE_MARKDOWN);

        if (found) {
          context.put("id", body.getInteger("id"));
          context.put("title", body.getString("title"));
          context.put("content", Processor.process(rawContent));
          context.put("rawContent", rawContent);
        } else {

//          context.put("id", -1);
//          context.put("title", "");
//          context.put("content", Processor.process(EMPTY_PAGE_MARKDOWN));
//          context.put("rawContent", EMPTY_PAGE_MARKDOWN);

          context.response().setStatusCode(404);
          context.response().end();
          return;
        }

        context.put("newPage", found ? "no" : "yes");
        context.put("timestamp", new Date().toString());

        templateEngine.render(context, "templates", "/pages/page_detail.ftl", ar -> {

          if (ar.succeeded()) {
            context.response().putHeader("Content-Type", "text/html");
            context.response().end(ar.result());
          } else {
            context.fail(ar.cause());
          }

        });

      } else {
        context.fail(reply.cause());
      }


    });

  }

  @Override
  public Vertx getVertx() {
    return this._vertx;
  }

}
