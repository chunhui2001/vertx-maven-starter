package com.shenmao.vertx.starter.passport;

import io.vertx.core.Vertx;
import io.vertx.ext.auth.shiro.ShiroAuth;
import io.vertx.ext.auth.shiro.ShiroAuthOptions;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.mgt.DefaultSecurityManager;
import org.apache.shiro.mgt.SecurityManager;

public class ShiroAuthProviderImpl {

  private Vertx vertx;
  private SecurityManager securityManager;
  private String rolePrefix = "role:";
  private String realmName;

  private ShiroAuthProviderImpl() {

  }



  public static ShiroAuth newInstance(Vertx vertx, ShiroAuthOptions options) {
    RealmImpl realm;
    switch(options.getType()) {
      case PROPERTIES:
        // realm = PropertiesAuthProvider.createRealm(options.getConfig());
        realm = new RealmImpl(vertx);
        break;
//      case LDAP:
//        realm = LDAPAuthProvider.createRealm(options.getConfig());
//        break;
      default:
        throw new IllegalArgumentException("Invalid shiro auth realm type: " + options.getType());
    }

    return new ShiroAuthProviderImpl().newInstance(vertx, realm, options);

  }


  public ShiroAuth newInstance(Vertx vertx, RealmImpl realm, ShiroAuthOptions options) {

    this.vertx = vertx;
    this.securityManager = new DefaultSecurityManager(realm);
    this.realmName = realm.getName();

    DefaultSecurityManager securityManager = new DefaultSecurityManager(realm);
    SecurityUtils.setSecurityManager(securityManager);

    realm.setSecurityManager(this.securityManager);


    return ShiroAuth.create(vertx, options);

  }

  Vertx getVertx() {
    return this.vertx;
  }

  SecurityManager getSecurityManager() {
    return this.securityManager;
  }

  String getRealmName() {
    return this.realmName;
  }
}

