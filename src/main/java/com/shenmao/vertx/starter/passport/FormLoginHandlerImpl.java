package com.shenmao.vertx.starter.passport;

import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.auth.AuthProvider;
import io.vertx.ext.auth.User;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.Session;
import io.vertx.ext.web.handler.FormLoginHandler;

public class FormLoginHandlerImpl implements FormLoginHandler {

  private static final Logger log = LoggerFactory.getLogger(FormLoginHandlerImpl.class);
  private final AuthProvider authProvider;
  private String usernameParam;
  private String passwordParam;
  private String returnURLParam;
  private String directLoggedInOKURL;
  private static final String DEFAULT_DIRECT_LOGGED_IN_OK_PAGE = "<html><body><h1>Login successful</h1></body></html>";

  public FormLoginHandler setUsernameParam(String usernameParam) {
    this.usernameParam = usernameParam;
    return this;
  }

  public FormLoginHandler setPasswordParam(String passwordParam) {
    this.passwordParam = passwordParam;
    return this;
  }

  public FormLoginHandler setReturnURLParam(String returnURLParam) {
    this.returnURLParam = returnURLParam;
    return this;
  }

  public FormLoginHandler setDirectLoggedInOKURL(String directLoggedInOKURL) {
    this.directLoggedInOKURL = directLoggedInOKURL;
    return this;
  }

  public FormLoginHandlerImpl(AuthProvider authProvider, String usernameParam, String passwordParam, String returnURLParam, String directLoggedInOKURL) {
    this.authProvider = authProvider;
    this.usernameParam = usernameParam;
    this.passwordParam = passwordParam;
    this.returnURLParam = returnURLParam;
    this.directLoggedInOKURL = directLoggedInOKURL;
  }

  public void handle(RoutingContext context) {
    HttpServerRequest req = context.request();
    if (req.method() != HttpMethod.POST) {
      context.fail(405);
    } else {
      if (!req.isExpectMultipart()) {
        throw new IllegalStateException("Form body not parsed - do you forget to include a BodyHandler?");
      }

      MultiMap params = req.formAttributes();
      String username = params.get(this.usernameParam);
      String password = params.get(this.passwordParam);

      if (username != null && password != null) {

        Session session = context.session();
        JsonObject authInfo = (new JsonObject()).put("username", username).put("password", password);

        this.authProvider.authenticate(authInfo, (res) -> {

          if (res.succeeded()) {

            User user = (User)res.result();
            context.setUser(user);

            if (session != null) {

              session.regenerateId();
              String returnURL = (String)session.remove(this.returnURLParam);

              if (returnURL != null) {
                this.doRedirect(req.response(), returnURL);
                return;
              }
            }

            if (this.directLoggedInOKURL != null) {
              this.doRedirect(req.response(), this.directLoggedInOKURL);
            } else {
              req.response().end(DEFAULT_DIRECT_LOGGED_IN_OK_PAGE);
            }
          } else {
            this.doRedirect(req.response(), "/login?error", 302);
          }

        });
      } else {
        log.warn("No username or password provided in form - did you forget to include a BodyHandler?");
        context.fail(400);
      }
    }

  }

  private void doRedirect(HttpServerResponse response, String url) {
    doRedirect(response, url, 302);
  }

  private void doRedirect(HttpServerResponse response, String url, int statusCode) {
    response.putHeader("location", url).setStatusCode(statusCode).end();
  }
}
