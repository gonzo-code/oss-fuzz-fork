
package org.jsoup.helper;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import org.jsoup.Connection;
import org.jsoup.helper.KeyVal;


public class ResponseExecuteFuzzer {
  public static void fuzzerInitialize() {
    // Initializing objects for fuzzing
  }

  public static void fuzzerTearDown() {
    // Tear down objects after fuzzing
  }

  public static void fuzzerTestOneInput(FuzzedDataProvider data) {
    int outer = data.consumeInt(1, 3);
    for (int i = 0; i < outer; i++) {
      HttpConnection.Request request = new HttpConnection.Request();
      try {
        String path = data.consumeString(100);
        URL url = new URL("http://localhost/" + path);
        request.url(url);
      } catch (MalformedURLException e) {
        continue;
      }
      request.method(data.pickValue(Connection.Method.values()));
      request.followRedirects(data.consumeBoolean());
      request.timeout(data.consumeInt(0, 1000));
      int mapSize = data.consumeInt(0, 5);
      Map<String, String> headerMap = new HashMap<>();
      for (int m = 0; m < mapSize; m++) {
        headerMap.put(data.consumeString(20), data.consumeString(40));
      }
      for (Map.Entry<String, String> entry : headerMap.entrySet()) {
        request.header(entry.getKey(), entry.getValue());
      }
      if (data.consumeBoolean()) {
        request.requestBody(new String(data.consumeBytes(200)));
      }
      int dataPairs = data.consumeInt(0, 3);
      for (int d = 0; d < dataPairs; d++) {
        request.data().add(KeyVal.create(data.consumeString(10), data.consumeString(20)));
      }

      HttpConnection.Response previousResponse = null;
      if (data.consumeBoolean()) {
        previousResponse = new HttpConnection.Response(new HttpConnection.Request());
        int prevMap = data.consumeInt(0, 5);
        for (int p = 0; p < prevMap; p++) {
          previousResponse.header(data.consumeString(20), data.consumeString(40));
        }
      }

      int inner = data.consumeInt(1, 3);
      for (int j = 0; j < inner; j++) {
        request.method(data.pickValue(Connection.Method.values()));
        request.header(data.consumeString(20), data.consumeString(40));
        try {
          HttpConnection.Response.execute(request, previousResponse);
        } catch (IOException | IllegalArgumentException e) {
          // ignore
        }
      }
    }
  }
}
