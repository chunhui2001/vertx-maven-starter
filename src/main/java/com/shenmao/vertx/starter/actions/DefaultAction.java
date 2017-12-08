package com.shenmao.vertx.starter.actions;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.rjeschke.txtmark.Processor;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.templ.FreeMarkerTemplateEngine;

import java.util.Date;

import static com.shenmao.vertx.starter.WikiDatabaseVerticle.EMPTY_PAGE_MARKDOWN;

public class DefaultAction implements Action {

  private final FreeMarkerTemplateEngine templateEngine = FreeMarkerTemplateEngine.create();
  private String wikiDbQueue = "wikidb.queue";

  private Vertx _vertx;

  public DefaultAction(Vertx vertx) {
    _vertx = vertx;
  }

  @Override
  public void indexHandler(RoutingContext context) {

    DeliveryOptions options = new DeliveryOptions().addHeader("action", "all-pages");

    _vertx.eventBus().send(wikiDbQueue, new JsonObject(), options, reply -> {

      if (reply.succeeded()) {

        JsonObject body = (JsonObject) reply.result().body();
        context.put("title", "Wiki home");
        context.put("pages", body.getJsonArray("pages").getList());

        templateEngine.render(context, "templates", "/index.ftl", ar -> {

          if (ar.succeeded()) {
            context.response().putHeader("Content-Type", "text/html");
          context.response().end(ar.result());
          } else { context.fail(ar.cause());
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

    DeliveryOptions options = new DeliveryOptions();

    if ("yes".equals(_new_page)) {
      options.addHeader("action", "create-page");
    } else {
      options.addHeader("action", "save-page");
    }

    _vertx.eventBus().send(wikiDbQueue, _params, options, reply -> {

      if (reply.succeeded()) {
        context.response().setStatusCode(303);
        context.response().putHeader("Location", "/wiki/" + _page_id);
        context.response().end();

      } else {
          context.fail(reply.cause());
      }

    });

  }

  @Override
  public void pageDeletionHandler(RoutingContext context) {

    String id = context.request().getParam("id");
    JsonObject request = new JsonObject().put("id", id);
    DeliveryOptions options = new DeliveryOptions().addHeader("action", "delete-page");

    _vertx.eventBus().send(wikiDbQueue, request, options, reply -> {

      if (reply.succeeded()) {
        context.response().setStatusCode(303);
        context.response().putHeader("Location", "/");
        context.response().end();
      } else {
        context.fail(reply.cause());
      }

    });

  }

  @Override
  public void pageCreateHandler(RoutingContext context) {

    String pageName = context.request().getParam("name");
    context.response().setStatusCode(303);

    if (pageName == null || pageName.isEmpty()) {
      context.response().putHeader("Location", "/");
      context.response().end();
    } else {

      JsonObject _data = new JsonObject().put("title", pageName).put("markdown", EMPTY_PAGE_MARKDOWN);

      _vertx.eventBus().send(wikiDbQueue, _data, new DeliveryOptions().addHeader("action", "create-page"), reply -> {

        if (reply.succeeded()) {

          if (reply.result().body() == null) {
            context.response().putHeader("Location", "/");
          } else {
            Long newPageId = (long) reply.result().body();
            context.response().putHeader("Location", "/wiki/" + newPageId);
          }

          context.response().end();

        } else {
          context.fail(reply.cause());
        }

      });

    }

  }

  @Override
  public void pageRenderingHandler(RoutingContext context) {

    String _page_id = context.request().getParam("id");
    JsonObject _data = new JsonObject().put("id", _page_id);
    DeliveryOptions options = new DeliveryOptions().addHeader("action", "get-page");

    _vertx.eventBus().send(wikiDbQueue, _data, options, reply -> {

      if (reply.succeeded()) {

        JsonObject body = (JsonObject) reply.result().body();

        boolean found = body.getBoolean("found");
        String rawContent = body.getString("rawContent", EMPTY_PAGE_MARKDOWN);

        if (found) {
          context.put("id", body.getInteger("id"));
          context.put("title", body.getString("title"));
          context.put("content", Processor.process(rawContent));
          context.put("rawContent", rawContent);
        } else {
          context.put("id", -1);
          context.put("title", "");
          context.put("content", Processor.process(EMPTY_PAGE_MARKDOWN));
          context.put("rawContent", EMPTY_PAGE_MARKDOWN);
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
