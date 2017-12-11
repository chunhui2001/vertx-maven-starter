package com.shenmao.vertx.starter.configuration;

import com.shenmao.vertx.starter.database.WikiDatabaseVerticle;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Properties;

public class SqlQueriesConfig {

  public enum SqlQuery {
    DROP_TABLE,
    CREATE_PAGES_TABLE,
    ALL_PAGES,
    GET_PAGE,
    CREATE_PAGE,
    SAVE_PAGE,
    DELETE_PAGE,
    LAST_INSERT_ID
  }

  private static String _config_file = null;
  private HashMap<SqlQuery, String> sqlQueries = null;
  private Properties queriesProps = null;

  private static SqlQueriesConfig _instance = null;

  public static void setConfigFile(String filePath) {
    _config_file = filePath;
  }

  public static synchronized SqlQueriesConfig newInstance() throws FileNotFoundException {

    if (_instance == null) {
      _instance = new SqlQueriesConfig(_config_file);
    }

    return _instance;

  }

  private SqlQueriesConfig(String configfile) throws FileNotFoundException {

    InputStream queriesInputStream = null;

    try {

      queriesInputStream = getClass().getResourceAsStream(configfile);

      queriesProps = new Properties();
      queriesProps.load(queriesInputStream);
      queriesInputStream.close();

    } catch (Exception e) {
      e.printStackTrace();
    }

    loadConfig();

  }


  private void loadConfig() {

    this.sqlQueries = new HashMap<>();

    sqlQueries.put(SqlQuery.DROP_TABLE, queriesProps.getProperty("drop-table"));
    sqlQueries.put(SqlQuery.CREATE_PAGES_TABLE, queriesProps.getProperty("create-pages-table"));
    sqlQueries.put(SqlQuery.ALL_PAGES, queriesProps.getProperty("all-pages"));
    sqlQueries.put(SqlQuery.GET_PAGE, queriesProps.getProperty("get-page"));
    sqlQueries.put(SqlQuery.CREATE_PAGE, queriesProps.getProperty("create-page"));
    sqlQueries.put(SqlQuery.SAVE_PAGE, queriesProps.getProperty("save-page"));
    sqlQueries.put(SqlQuery.DELETE_PAGE, queriesProps.getProperty("delete-page"));
    sqlQueries.put(SqlQuery.LAST_INSERT_ID, queriesProps.getProperty("get-last-increment-id"));

  }

  public HashMap<SqlQuery, String> getQueries() {

    if (this.sqlQueries == null) loadConfig();

    return this.sqlQueries;

  }
}
