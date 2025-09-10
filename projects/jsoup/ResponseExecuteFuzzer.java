package org.jsoup.helper;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import org.jsoup.Connection;
import org.jsoup.helper.HttpConnection;

public class ResponseExecuteFuzzer {
  public static void fuzzerInitialize() {}

  public static void fuzzerTearDown() {}

  public static void fuzzerTestOneInput(FuzzedDataProvider data) {
    // Generate a simple request and URL backed by a custom URLStreamHandler so that
    // HttpConnection.Response.execute does not perform a real network request.
    String path = data.consumeString(100);
    int headerCount = data.consumeInt(0, 3);
    String[] headerKeys = new String[headerCount];
    String[] headerVals = new String[headerCount];
    for (int i = 0; i < headerCount; i++) {
      headerKeys[i] = data.consumeString(20);
      headerVals[i] = data.consumeString(50);
    }
    byte[] body = data.consumeRemainingAsBytes();

    URL url;
    try {
      url = new URL(null, "http://example.com/" + path,
          new FuzzURLStreamHandler(body, headerKeys, headerVals));
    } catch (MalformedURLException e) {
      return;
    }

    HttpConnection.Request request = new HttpConnection.Request();
    request.url(url);
    request.method(data.pickValue(Connection.Method.values()));

    int runs = data.consumeInt(1, 3);
    for (int i = 0; i < runs; i++) {
      try {
        HttpConnection.Response.execute(request, null);
      } catch (IOException | IllegalArgumentException e) {
        // ignore
      }
    }
  }

  private static final class FuzzURLStreamHandler extends URLStreamHandler {
    private final byte[] body;
    private final String[] headerKeys;
    private final String[] headerVals;

    FuzzURLStreamHandler(byte[] body, String[] headerKeys, String[] headerVals) {
      this.body = body;
      this.headerKeys = headerKeys;
      this.headerVals = headerVals;
    }

    @Override
    protected URLConnection openConnection(URL u) {
      return new FuzzHttpURLConnection(u, body, headerKeys, headerVals);
    }
  }

  private static final class FuzzHttpURLConnection extends HttpURLConnection {
    private final byte[] body;
    private final String[] headerKeys;
    private final String[] headerVals;

    protected FuzzHttpURLConnection(URL url, byte[] body, String[] headerKeys, String[] headerVals) {
      super(url);
      this.body = body;
      this.headerKeys = headerKeys;
      this.headerVals = headerVals;
    }

    @Override
    public void connect() {}

    @Override
    public void disconnect() {}

    @Override
    public boolean usingProxy() {
      return false;
    }

    @Override
    public InputStream getInputStream() {
      return new ByteArrayInputStream(body);
    }

    @Override
    public OutputStream getOutputStream() {
      return new ByteArrayOutputStream();
    }

    @Override
    public int getResponseCode() {
      return 200;
    }

    @Override
    public String getResponseMessage() {
      return "OK";
    }

    @Override
    public String getContentType() {
      String ct = getHeaderField("Content-Type");
      return ct != null ? ct : "text/html";
    }

    @Override
    public int getContentLength() {
      return body.length;
    }

    @Override
    public String getHeaderField(String name) {
      for (int i = 0; i < headerKeys.length; i++) {
        if (headerKeys[i] != null && headerKeys[i].equalsIgnoreCase(name)) {
          return headerVals[i];
        }
      }
      return null;
    }

    @Override
    public String getHeaderFieldKey(int n) {
      return n < headerKeys.length ? headerKeys[n] : null;
    }

    @Override
    public String getHeaderField(int n) {
      return n < headerVals.length ? headerVals[n] : null;
    }
  }
}
