package com.shenmao.vertx.starter.database.wikipage;

import io.vertx.rxjava.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.rxjava.ext.jdbc.JDBCClient;
import io.vertx.serviceproxy.ServiceBinder;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Properties;

public class WikiPageDatabaseVerticle extends AbstractVerticle {

  public static final String CONFIG_WIKIDB_JDBC_URL = "wikidb.jdbc.url";
  public static final String CONFIG_WIKIDB_JDBC_DRIVER_CLASS = "wikidb.jdbc.driver_class";
  public static final String CONFIG_WIKIDB_JDBC_MAX_POOL_SIZE = "wikidb.jdbc.max_pool_size";
  public static final String CONFIG_WIKIDB_SQL_QUERIES_RESOURCE_FILE = "wikidb.sqlqueries.resource.file";
  public static final String CONFIG_WIKIDB_QUEUE = "wikidb.queue";
  private static final Logger LOGGER = LoggerFactory.getLogger(WikiPageDatabaseVerticle.class);



  private JDBCClient dbClient;

  private HashMap<WikiSqlQuery, String> loadSqlQueries() throws IOException {

    String queriesFile = config().getString(CONFIG_WIKIDB_SQL_QUERIES_RESOURCE_FILE);
    HashMap<WikiSqlQuery, String> sqlQueriesWikiPages = new HashMap<>();

    InputStream queriesInputStream;
    if (queriesFile != null) {
      queriesInputStream = new FileInputStream(queriesFile); } else {
      queriesInputStream = getClass().getResourceAsStream("/properties/wikipage-db-queries.properties");
    }

    Properties queriesProps = new Properties(); queriesProps.load(queriesInputStream); queriesInputStream.close();
    sqlQueriesWikiPages.put(WikiSqlQuery.CREATE_WIKI_PAGES_TABLE, queriesProps.getProperty("create-wikipages-table"));
    sqlQueriesWikiPages.put(WikiSqlQuery.ALL_WIKI_PAGES, queriesProps.getProperty("all-wiki-pages"));
    sqlQueriesWikiPages.put(WikiSqlQuery.GET_WIKI_PAGE, queriesProps.getProperty("get-wiki-page"));
    sqlQueriesWikiPages.put(WikiSqlQuery.CREATE_WIKI_PAGE, queriesProps.getProperty("create-wiki-page"));
    sqlQueriesWikiPages.put(WikiSqlQuery.SAVE_WIKI_PAGE, queriesProps.getProperty("save-wiki-page"));
    sqlQueriesWikiPages.put(WikiSqlQuery.DELETE_WIKI_PAGE, queriesProps.getProperty("delete-wiki-page"));

    return  sqlQueriesWikiPages;
  }


  @Override
  public void start(Future<Void> startFuture) throws Exception {


    dbClient = JDBCClient.createShared(vertx, new JsonObject()
      .put("url", config().getString(CONFIG_WIKIDB_JDBC_URL, "jdbc:hsqldb:file:db_wikipage/wiki_pages"))
      .put("driver_class", config().getString(CONFIG_WIKIDB_JDBC_DRIVER_CLASS, "org.hsqldb.jdbcDriver"))
      .put("max_pool_size", config().getInteger(CONFIG_WIKIDB_JDBC_MAX_POOL_SIZE, 30)));

    WikiPageDbService.create(dbClient, loadSqlQueries(), ready -> {

      if (ready.succeeded()) {

        /*ServiceProxyBuilder serviceProxyBuilder = new ServiceProxyBuilder(vertx)
          .setAddress(CONFIG_WIKIDB_QUEUE);
        serviceProxyBuilder.build(WikiPageDbService.class); */

        /*io.vertx.serviceproxy.ProxyHelper.registerService(
          WikiPageDbService.class, vertx, ready.result(), CONFIG_WIKIDB_QUEUE);*/

        (new ServiceBinder(vertx.getDelegate())).setAddress(CONFIG_WIKIDB_QUEUE).register(WikiPageDbService.class, ready.result());

        startFuture.complete();

      } else {

        startFuture.fail(ready.cause());
      }
    });

  }

}
