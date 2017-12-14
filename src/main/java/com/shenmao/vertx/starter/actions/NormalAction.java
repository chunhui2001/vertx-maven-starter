package com.shenmao.vertx.starter.actions;

import com.shenmao.vertx.starter.database.WikiDatabaseService;
import com.shenmao.vertx.starter.database.WikiDatabaseVerticle;
import com.shenmao.vertx.starter.passport.JWTAuthenticated;
import com.shenmao.vertx.starter.passport.ShiroAuthenticate;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.auth.AuthProvider;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTOptions;
import io.vertx.ext.web.RoutingContext;
import org.apache.commons.codec.binary.Base64;

public class NormalAction {

  private static final Logger LOGGER = LoggerFactory.getLogger(NormalAction.class);
  private static final String wikiDbQueue = WikiDatabaseVerticle.CONFIG_WIKIDB_QUEUE;

  private WikiDatabaseService dbService;

  private Vertx _vertx;

  public NormalAction(Vertx vertx) {
    _vertx = vertx;
    dbService = WikiDatabaseService.createProxy(vertx, wikiDbQueue);
  }

  public void loginHandler(RoutingContext context) {

    if (context.user() != null) {
      context.response().setStatusCode(303);
      context.response().putHeader("Location", "/index");
      context.response().end();
      return;
    }

    boolean error = context.queryParams().contains("error");

    context.put("title", "Login");
    context.put("error", error);

    ContextResponse.write(context, "/login.ftl");


  }

  public void accessTokenHandler(RoutingContext context) {

    // curl -v -u zch:zch http://localhost:9180/access_token
    // curl -k -u zch:zch https://localhost:9180/access_token # ssl
    // http --auth zch:zch --auth-type basic --verbose --verify no GET https://localhost:9180/access_token

    final String _authString = context.request().getHeader("Authorization");
    final String _base64AuthStr = _authString == null || _authString.isEmpty() ? "" : _authString.split(" ")[1];

    if (_authString == null || _authString.isEmpty()
      || !_authString.split(" ")[0].equals("Basic")
      || !Base64.isBase64(_base64AuthStr)) {
      context.fail(401);
      return;
    }



    final String _usernameAndPasswd = new String(Base64.decodeBase64(_base64AuthStr));

    JsonObject creds = new JsonObject()
      .put("username", _usernameAndPasswd.split(":")[0])
      .put("password", _usernameAndPasswd.split(":")[1]);

    AuthProvider auth = ShiroAuthenticate.newInstance(_vertx);

    auth.authenticate(creds, authResult -> {

      if (authResult.succeeded()) {

        User user = authResult.result();

        user.isAuthorized("create", canCreate -> {

          user.isAuthorized("delete", canDelete -> {

            user.isAuthorized("update", canUpdate -> {

              String token = JWTAuthenticated.jwtAuth().generateToken(
                    new JsonObject()
                      .put("username", _usernameAndPasswd.split(":")[0])
                      .put("canCreate", canCreate.succeeded() && canCreate.result())
                      .put("canDelete", canDelete.succeeded() && canDelete.result())
                      .put("canUpdate", canUpdate.succeeded() && canUpdate.result()),
                      new JWTOptions() .setSubject("Wiki API") .setIssuer("Vert.x")
              );

                context.response().putHeader("Content-Type", "text/plain").end(token);
            });

          });

        });

      } else {
        System.out.println("authenticate failed!");
        context.fail(401);
      }

    });

  }


}
