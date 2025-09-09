package org.jsoup.helper;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import org.jsoup.Connection;

public class ResponseExecuteFuzzer {
  public static void fuzzerTestOneInput(FuzzedDataProvider data) {
    HttpConnection.Request request = new HttpConnection.Request();
    try {
      request.url(new URL("http://localhost/" + data.consumeString(100)));
    } catch (MalformedURLException e) {
      return;
    }
    request.method(data.pickValue(Connection.Method.values()));
    try {
      HttpConnection.Response.execute(request, null);
    } catch (IOException | IllegalArgumentException e) {
      // ignore
    }
  }
}
