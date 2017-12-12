package com.shenmao.vertx.starter.passport;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.AuthProvider;
import io.vertx.ext.auth.shiro.ShiroAuth;
import io.vertx.ext.auth.shiro.ShiroAuthOptions;
import io.vertx.ext.auth.shiro.ShiroAuthRealmType;

public class ShiroAuthenticate {

  public static AuthProvider newInstance(Vertx vertx) {
    return ShiroAuth.create(vertx, new ShiroAuthOptions()
      .setType(ShiroAuthRealmType.PROPERTIES)
      .setConfig(new JsonObject()
        .put("properties_path", "classpath:properties/web-users.properties")));
  }
}
