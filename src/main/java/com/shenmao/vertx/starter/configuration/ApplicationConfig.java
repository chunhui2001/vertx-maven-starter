package com.shenmao.vertx.starter.configuration;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Properties;

public class ApplicationConfig {

  public enum AppConfig {
    APP_PORT,
    APP_HOST
  }

  private static String _config_file = null;
  private HashMap<AppConfig, String> appConfig = null;
  private Properties appProps = null;
  private static ApplicationConfig _instance;

  public static void setConfigFile(String filePath) {
    _config_file = filePath;
  }

  public static synchronized ApplicationConfig newInstance() throws FileNotFoundException {

    if (_instance == null) {
      _instance = new ApplicationConfig(_config_file);
    }

    return _instance;

  }

  private ApplicationConfig(String configfile) throws FileNotFoundException {

    InputStream queriesInputStream = null;

    try {

      queriesInputStream = getClass().getResourceAsStream(configfile);

      appProps = new Properties();
      appProps.load(queriesInputStream);
      queriesInputStream.close();

    } catch (Exception e) {
      e.printStackTrace();
    }

    loadConfig();

  }

  private void loadConfig() {

    this.appConfig = new HashMap<>();

    appConfig.put(AppConfig.APP_PORT, appProps.getProperty("app_port"));
    appConfig.put(AppConfig.APP_HOST, appProps.getProperty("app_host"));

  }

  public HashMap<AppConfig, String> getConfig() {
    return this.appConfig;
  }

  public String getUrl(boolean isSSL) {
    return "http" + (isSSL ? "s" : "") +"://" +
        this.appConfig.get(AppConfig.APP_HOST) + ":" + this.appConfig.get(AppConfig.APP_PORT);
  }

}
