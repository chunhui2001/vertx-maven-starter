package com.shenmao.vertx.starter.passport;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.VertxException;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.shiro.ShiroAuth;
import io.vertx.ext.auth.shiro.ShiroAuthOptions;
import io.vertx.ext.auth.shiro.impl.LDAPAuthProvider;
import io.vertx.ext.auth.shiro.impl.PropertiesAuthProvider;
import io.vertx.ext.auth.shiro.impl.ShiroUser;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.*;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.mgt.DefaultSecurityManager;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.realm.Realm;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.subject.SubjectContext;
import org.apache.shiro.subject.support.DefaultSubjectContext;

import java.util.HashSet;
import java.util.Set;

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

