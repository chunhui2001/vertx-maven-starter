package com.shenmao.vertx.starter.passport;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.KeyStoreOptions;
import io.vertx.ext.auth.SecretOptions;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
//import io.vertx.ext.auth.jwt.impl.JWTAuthProviderImpl;

public class JWTAuthenticated {

  static JWTAuth jwtAuth = null;

  /**
   * Install httpid on mac
   * ruby -e "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/master/install)" < /dev/null 2> /dev/null
   * brew install httpie
   */

  // keytool -genseckey -keystore keystore.jceks -storetype jceks -storepass secret -keyalg HMacSHA256 -keysize 2048 -alias HS256 -keypass secret
  // keytool -genseckey -keystore keystore.jceks -storetype jceks -storepass secret -keyalg HMacSHA384 -keysize 2048 -alias HS384 -keypass secret
  // keytool -genseckey -keystore keystore.jceks -storetype jceks -storepass secret -keyalg HMacSHA512 -keysize 2048 -alias HS512 -keypass secret

  // keytool -genkey -keystore keystore.jceks -storetype jceks -storepass secret -keyalg RSA -keysize 2048 -alias RS256 -keypass secret -sigalg SHA256withRSA -dname "CN=,OU=,O=,L=,ST=,C=" -validity 360
  // keytool -genkey -keystore keystore.jceks -storetype jceks -storepass secret -keyalg RSA -keysize 2048 -alias RS384 -keypass secret -sigalg SHA384withRSA -dname "CN=,OU=,O=,L=,ST=,C=" -validity 360
  // keytool -genkey -keystore keystore.jceks -storetype jceks -storepass secret -keyalg RSA -keysize 2048 -alias RS512 -keypass secret -sigalg SHA512withRSA -dname "CN=,OU=,O=,L=,ST=,C=" -validity 360

  // keytool -genkeypair -keystore keystore.jceks -storetype jceks -storepass secret -keyalg EC -keysize 256 -alias ES256 -keypass secret -sigalg SHA256withECDSA -dname "CN=,OU=,O=,L=,ST=,C=" -validity 360
  // keytool -genkeypair -keystore keystore.jceks -storetype jceks -storepass secret -keyalg EC -keysize 256 -alias ES384 -keypass secret -sigalg SHA384withECDSA -dname "CN=,OU=,O=,L=,ST=,C=" -validity 360
  // keytool -genkeypair -keystore keystore.jceks -storetype jceks -storepass secret -keyalg EC -keysize 256 -alias ES512 -keypass secret -sigalg SHA512withECDSA -dname "CN=,OU=,O=,L=,ST=,C=" -validity 360

  // keytool -genkey -alias test -keyalg RSA -keystore server-keystore.jks -keysize 2048 -validity 360 -dname CN=localhost -keypass secret -storepass secret
  public static JWTAuth newInstance(Vertx vertx) {


//    return JWTAuth.create(vertx, new JsonObject() .put("keyStore", new JsonObject()
//      .put("path", "ssh_keys/keystore.jceks")
//      .put("type", "jceks")
//      .put("password", "secret")));

    if (jwtAuth == null) {
//      jwtAuth = JWTAuth.create(vertx, new JWTAuthOptions()
//        .setKeyStore(new KeyStoreOptions()
////        .setPath("ssh_keys/keystore.jceks")
//          .setPath("ssh_keys/server-keystore.jks")
//          .setType("jceks")
//          .setPassword("secret")));

      jwtAuth = new JWTAuthProviderImpl(vertx, new JWTAuthOptions()
        .setKeyStore(new KeyStoreOptions()
//        .setPath("ssh_keys/keystore.jceks")
          .setPath("ssh_keys/server-keystore.jks")
          .setType("jceks")
          .setPassword("secret")));

    }

    return jwtAuth;

  }

  public static JWTAuth jwtAuth() {
    return jwtAuth;
  }

}
