package com.shenmao.vertx.starter.sessionstore;

import io.vertx.core.Vertx;
import io.vertx.ext.web.sstore.SessionStore;

public interface RedisSessionStore extends SessionStore {

  long DEFAULT_RETRY_TIMEOUT = 2 * 1000;

  String DEFAULT_SESSION_MAP_NAME = "vertx-web.sessions";

  static RedisSessionStore create(Vertx vertx) {
    return new RedisSessionStoreImpl(vertx, DEFAULT_SESSION_MAP_NAME, DEFAULT_RETRY_TIMEOUT);
  }

  static RedisSessionStore create(Vertx vertx, String sessionMapName) {
    return new RedisSessionStoreImpl(vertx, sessionMapName, DEFAULT_RETRY_TIMEOUT);
  }

  static RedisSessionStore create(Vertx vertx, String sessionMapName, long reaperInterval) {
    return new RedisSessionStoreImpl(vertx, sessionMapName, reaperInterval);
  }

  RedisSessionStore host(String host);

  RedisSessionStore port(int port);

  RedisSessionStore auth(String pwd);

}
