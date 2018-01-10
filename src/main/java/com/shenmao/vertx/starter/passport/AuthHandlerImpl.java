package com.shenmao.vertx.starter.passport;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.AuthProvider;
import io.vertx.ext.auth.KeyStoreOptions;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
import io.vertx.ext.auth.shiro.ShiroAuthOptions;
import io.vertx.ext.auth.shiro.ShiroAuthRealmType;
import io.vertx.ext.web.handler.*;
import io.vertx.ext.web.handler.impl.BasicAuthHandlerImpl;

public class AuthHandlerImpl {

  private static AuthProvider _authProvider;
  private static JWTAuth _jwtAuthProvider;
  private static JWTAuthOptions jwtAuthOptions = new JWTAuthOptions()
    .setKeyStore(new KeyStoreOptions()
      .setPath("keystore.jceks")
//        .setPath("ssh_keys/server-keystore.jks")
      .setType("jceks")
      .setPassword("secret"));

  public static synchronized AuthHandler create(Vertx vertx) {

    ShiroAuthOptions shiroAuthOptions = new ShiroAuthOptions().setType(ShiroAuthRealmType.PROPERTIES).setConfig(new JsonObject().put("properties_path", "classpath:app-users.properties"));
    AuthProvider shiroAuthProvider = ShiroAuthProviderImpl.newInstance(vertx, shiroAuthOptions);
    JWTAuth jwtAuthProvider = JWTAuth.create(vertx, jwtAuthOptions);

    AuthHandler jwtAuthHandler = JWTAuthHandler.create(jwtAuthProvider);
    AuthHandler basicAuthHandler = BasicAuthHandler.create(shiroAuthProvider);
    AuthHandler redirectAuthHandler = RedirectAuthHandler.create(shiroAuthProvider, "/login");

    ChainAuthHandler chain = ChainAuthHandler.create();

    chain.append(jwtAuthHandler);
    chain.append(basicAuthHandler);
    chain.append(redirectAuthHandler);

    _authProvider = shiroAuthProvider;
    _jwtAuthProvider = jwtAuthProvider;

    // chain.addAuthority("role:admin");

    return chain;
//    return redirectAuthHandler;

  }

  public static AuthProvider getAuthProvider() {
    return _authProvider;
  }

  public static JWTAuth getJWTAuthProvider() {
    return _jwtAuthProvider;
  }

}
