package com.shenmao.vertx.starter.actions;

import com.github.rjeschke.txtmark.Processor;
import com.shenmao.vertx.starter.database.WikiDatabaseService;
import com.shenmao.vertx.starter.database.WikiDatabaseVerticle;
import com.shenmao.vertx.starter.passport.ShiroRealm;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.ext.web.codec.BodyCodec;
import io.vertx.ext.web.templ.FreeMarkerTemplateEngine;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static com.shenmao.vertx.starter.database.WikiDatabaseVerticle.EMPTY_PAGE_MARKDOWN;

public class DefaultAction implements Action {

  private static final Logger LOGGER = LoggerFactory.getLogger(DefaultAction.class);
  private static final String wikiDbQueue = WikiDatabaseVerticle.CONFIG_WIKIDB_QUEUE;


  private WikiDatabaseService dbService;

  private Vertx _vertx;

  public DefaultAction(Vertx vertx) {
    _vertx = vertx;
    dbService = WikiDatabaseService.createProxy(vertx, wikiDbQueue);
  }

  @Override
  public void indexHandler(RoutingContext context) {


    context.user().isAuthorized(ShiroRealm.Permission.CREATE.toString(),res -> {

      dbService.fetchAllPages(reply -> {

        if (reply.succeeded()) {

          context.put("title", "Wiki Home");
          context.put("content", reply.result());
          context.put("canCreatePage", res.succeeded() && res.result());

          ContextResponse.write(context, "/index.ftl");

        } else {
          context.fail(reply.cause());
        }

      });


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

          context.response().end();
        } else {
          ContextResponse.write(context, "/wiki/" + reply.result(), reply.result(), 301);
        }


      } else {
        context.fail(reply.cause());
      }

    };

    Handler<AsyncResult<Void>> updateHandler = reply -> {

      if (reply.succeeded()) {

        ContextResponse.write(context, "/wiki/" + _page_id, _page_id, 301);

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

    dbService.deletePage(id, reply -> {

      if (reply.succeeded()) {
        ContextResponse.write(context, "/", id, 301);
      } else {
        context.fail(reply.cause());
      }

    });

  }

  @Override
  public void pageCreateHandler(RoutingContext context) {

    String pageName = context.request().getParam("name");

    dbService.createPage(pageName, EMPTY_PAGE_MARKDOWN, reply -> {

      if (reply.succeeded()) {
        ContextResponse.write(context, "/wiki/" + (long) reply.result(), (long) reply.result(), 301);
      } else {
        context.fail(reply.cause());
      }

    });

  }

  @Override
  public void pageRenderingHandler(RoutingContext context) {

    long _page_id = Long.parseLong(context.request().getParam("id"));

    dbService.fetchPage(_page_id, reply -> {

      if (reply.succeeded()) {

        JsonObject body = reply.result();

        boolean found = body.getBoolean("found");
        String rawContent = body.getString("rawContent", EMPTY_PAGE_MARKDOWN);

        context.put("newPage", found ? "no" : "yes");
        context.put("timestamp", new Date().toString());

        if (found) {

          context.put("id", body.getInteger("id"));
          context.put("title", body.getString("title"));
          context.put("content", Processor.process(rawContent));
          context.put("rawContent", rawContent);

          ContextResponse.write(context, "/pages/page_detail.ftl");
        } else {

          ContextResponse.write(context, "/pages/page_detail.ftl", 404);
        }

      } else {
        context.fail(reply.cause());
      }


    });

  }

  @Override
  public void backupHandler(RoutingContext context) {

    dbService.fetchAllPages(reply -> {

      if (!reply.succeeded()) {
        context.fail(reply.cause());
      } else {

        JsonObject filesListObject = new JsonObject();


        JsonObject gistPayload = new JsonObject()
              .put("files", filesListObject)
              .put("description", "A wiki backup")
              .put("public", true);

        WebClient webClient = WebClient.create(_vertx,
              new WebClientOptions().setSsl(true).setUserAgent("vert-x3"));

        webClient.post(443, "api.github.com", "/gists")
          .putHeader("Accept", "application/vnd.github.v3+json")
          .putHeader("Content-Type", "application/json")
          .as(BodyCodec.jsonObject())
          .sendJsonObject(gistPayload, ar -> {

          if (ar.succeeded()) {

            HttpResponse<JsonObject> response = ar.result();

            if (response.statusCode() == 201) {

              context.put("backup_gist_url", response.body().getString("html_url"));

              System.out.println(context.get("backup_gist_url") + " , backup_gist_url 777");
              indexHandler(context);

            } else {

              StringBuilder message = new StringBuilder()
                .append("Could not backup the wiki: ")
                .append(response.statusMessage());

              JsonObject body = response.body();

              if (body != null) {
                message.append(System.getProperty("line.separator")).append(body.encodePrettily());
              }  {
                LOGGER.error(message.toString());
                context.fail(502);
              }

            }

          } else {

            LOGGER.error("Vert.x HTTP Client error", ar.cause());
            context.fail(ar.cause());
          }

        });

      }

    });

  }


  @Override
  public Vertx getVertx() {
    return this._vertx;
  }

}
