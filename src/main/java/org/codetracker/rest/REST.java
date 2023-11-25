package org.codetracker.rest;

import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.handlers.PathHandler;

public class REST {

  // check if the reported issues have been fixed
  public static boolean checkReported = false;
  // repository where the change history issue is to be reported
  public static String issueRepo = "jodavimehran/code-tracker";

  public static void main(String[] args) {
    PathHandler path = Handlers.path().addPrefixPath("/api", new RESTHandler());

    Undertow server = Undertow
      .builder()
      .addHttpListener(5000, "0.0.0.0")
      .setHandler(path)
      .build();

    server.start();
  }
}
