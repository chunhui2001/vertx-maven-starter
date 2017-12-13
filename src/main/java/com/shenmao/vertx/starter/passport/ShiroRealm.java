package com.shenmao.vertx.starter.passport;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.AuthProvider;
import io.vertx.ext.auth.shiro.ShiroAuthOptions;
import io.vertx.ext.auth.shiro.ShiroAuthRealmType;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.mgt.DefaultSecurityManager;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.realm.Realm;
import org.apache.shiro.subject.PrincipalCollection;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class ShiroRealm {

  public static boolean ENABLE_REALM = true;
  public static final String _SHIRO_INI_FILE = "classpath:properties/web-users.properties";

  public enum UserRoles {
    ADMIN, EDITER, WRITER
  }

  public enum Permission{
    ANY, CREATE, DELETE, UPDATE
  }

  public static Map<UserRoles, Set<Permission>> rolePermiss () {

    Map<UserRoles, Set<Permission>> result = new HashMap<>();

    result.put(UserRoles.ADMIN,
      new HashSet<Permission>() {{
        add(Permission.ANY);
      }}
    );

    result.put(UserRoles.EDITER,
      new HashSet<Permission>() {{
        add(Permission.CREATE);
        add(Permission.DELETE);
        add(Permission.UPDATE);
      }}
    );

    result.put(UserRoles.WRITER,
      new HashSet<Permission>() {{
        add(Permission.UPDATE);
      }}
    );

    return result;

  }

  public static ShiroAuthOptions putRealm() {

    ShiroAuthOptions shiroAuthOptions = new ShiroAuthOptions()
      .setType(ShiroAuthRealmType.PROPERTIES)
      .setConfig(new JsonObject()
        .put("properties_path", _SHIRO_INI_FILE));

    if (!ENABLE_REALM) return shiroAuthOptions;

    Realm realm = new AuthorizingRealm() {

      @Override
      protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principalCollection) {

        String userName = (String) principalCollection.getPrimaryPrincipal();
        SimpleAuthorizationInfo authorizationInfo = new SimpleAuthorizationInfo();

        Set<String> roles = new HashSet<>();

        // 根据用户标识取得权限
        roles.add(UserRoles.EDITER.toString());

        authorizationInfo.setRoles(roles);
        authorizationInfo.setStringPermissions(
          rolePermiss ().get(UserRoles.EDITER).stream().map(p -> p.toString()).collect(Collectors.toSet()));

        return authorizationInfo;

      }

      @Override
      protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken authenticationToken) throws AuthenticationException {

        String username = (String) authenticationToken.getPrincipal();
        String password = new String((char[]) authenticationToken.getCredentials());

        // 验证用户名密码
        if (null != username && null != password && username.equals(password)) {
          return new SimpleAuthenticationInfo(username, password, getName());
        }

        return null;

      }

    };

    DefaultSecurityManager securityManager = new DefaultSecurityManager(realm);
    SecurityUtils.setSecurityManager(securityManager);


    return shiroAuthOptions;

  }
}
