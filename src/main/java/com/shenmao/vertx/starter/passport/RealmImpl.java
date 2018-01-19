package com.shenmao.vertx.starter.passport;

import io.vertx.core.VertxException;
import io.vertx.ext.auth.shiro.impl.PropertiesAuthProvider;
import io.vertx.ext.auth.shiro.impl.ShiroUser;
import io.vertx.core.Vertx;
import org.apache.shiro.authc.*;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.realm.Realm;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.subject.SubjectContext;
import org.apache.shiro.subject.support.DefaultSubjectContext;

import java.util.HashSet;
import java.util.Set;

public class RealmImpl extends AuthorizingRealm {

  private Vertx vertx;
  SecurityManager securityManager;

  public RealmImpl(Vertx vertx) {
    this.vertx =vertx;
  }

  public void setSecurityManager(SecurityManager securityManager) {
    this.securityManager = securityManager;
  }

  @Override
  protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principalCollection) {

    String userName = (String) principalCollection.getPrimaryPrincipal();
    SimpleAuthorizationInfo authorizationInfo = new SimpleAuthorizationInfo();

    Set<String> roles = new HashSet<>();

    // 根据用户标识取得权限
//        roles.add(UserRoles.EDITER.toString());
//
//        authorizationInfo.setRoles(roles);
//        authorizationInfo.setStringPermissions(
//          rolePermiss ().get(UserRoles.EDITER).stream().map(p -> p.toString()).collect(Collectors.toSet()));

    return authorizationInfo;

  }

  @Override
  protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken authenticationToken) throws AuthenticationException {

    String username = (String) authenticationToken.getPrincipal();
    String password = new String((char[]) authenticationToken.getCredentials());

    SubjectContext subjectContext = new DefaultSubjectContext();
    Subject subject = securityManager.createSubject(subjectContext);

    UsernamePasswordToken token = new UsernamePasswordToken(username, password);

    // 验证用户名密码
    if (null != username && null != password && !username.trim().isEmpty() && username.equals(password)) {

//      try {
//        subject.login(token);
//      } catch (AuthenticationException var9) {
//        throw new VertxException(var9);
//      }

      return new SimpleAuthenticationInfo(username, password, getName());
    }

    return null;

  }

}
