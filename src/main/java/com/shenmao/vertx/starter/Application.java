package com.shenmao.vertx.starter;

import com.shenmao.vertx.starter.configuration.ApplicationConfig;
import com.shenmao.vertx.starter.configuration.SqlQueriesConfig;

import java.io.FileNotFoundException;
import java.util.HashMap;

public class Application {

  static SqlQueriesConfig sqlQueriesConfig;
  static ApplicationConfig applicationConfig;

  static {

    SqlQueriesConfig.setConfigFile("/properties/db-queries.properties");
    ApplicationConfig.setConfigFile("/properties/application.properties");

    try {
      applicationConfig = ApplicationConfig.newInstance();
      sqlQueriesConfig = SqlQueriesConfig.newInstance();
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }

  }

  public static ApplicationConfig applicationConfigInstance() {
    return applicationConfig;
  }

  public static HashMap<ApplicationConfig.AppConfig, String> getAppConfig() {
    return applicationConfig.getConfig();

  }

  public static HashMap<SqlQueriesConfig.SqlQuery, String> getSqlQueriesConfig() {
    return sqlQueriesConfig.getQueries();

  }

}
