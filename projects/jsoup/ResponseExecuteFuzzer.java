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
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPOutputStream;
import org.jsoup.Connection;
import org.jsoup.helper.HttpConnection;

public class ResponseExecuteFuzzer {
  public static void fuzzerInitialize() {}

  public static void fuzzerTearDown() {}

  public static void fuzzerTestOneInput(FuzzedDataProvider data) {
    // Generate a simple request and URL backed by a custom URLStreamHandler so that
    // HttpConnection.Response.execute does not perform a real network request.
    String path = data.consumeString(100);
    int status = data.consumeInt(100, 599);
    String contentType = data.pickValue(new String[] {
        "text/html", "application/xml", "application/octet-stream", data.consumeString(20)});
    String contentEncoding = data.pickValue(new String[] {"", "gzip", "deflate"});

    boolean isRedirect = status >= 300 && status < 400;
    String location = null;
    int headerCount = data.consumeInt(0, 3);
    int extra = 1; // content-type
    if (!contentEncoding.isEmpty()) extra++;
    if (isRedirect) extra++;
    String[] headerKeys = new String[headerCount + extra];
    String[] headerVals = new String[headerCount + extra];
    int h = 0;
    headerKeys[h] = "Content-Type";
    headerVals[h++] = contentType;
    if (!contentEncoding.isEmpty()) {
      headerKeys[h] = "Content-Encoding";
      headerVals[h++] = contentEncoding;
    }
    if (isRedirect) {
      location = "http://example.com/" + data.consumeString(100);
      headerKeys[h] = "Location";
      headerVals[h++] = location;
    }
    for (int i = 0; i < headerCount; i++) {
      headerKeys[h] = data.consumeString(20);
      headerVals[h] = data.consumeString(50);
      h++;
    }

    byte[] body = data.consumeRemainingAsBytes();
    byte[] encBody = body;
    try {
      if ("gzip".equalsIgnoreCase(contentEncoding)) {
        encBody = gzip(body);
      } else if ("deflate".equalsIgnoreCase(contentEncoding)) {
        encBody = deflate(body);
      }
    } catch (IOException e) {
      // ignore compression errors
    }

    URL url;
    try {
      url = new URL(null, "http://example.com/" + path,
          new FuzzURLStreamHandler(encBody, headerKeys, headerVals, status));
    } catch (MalformedURLException e) {
      return;
    }

    HttpConnection.Request request = new HttpConnection.Request();
    request.url(url);
    request.method(data.pickValue(Connection.Method.values()));
    request.ignoreHttpErrors(data.consumeBoolean());
    request.ignoreContentType(data.consumeBoolean());
    request.followRedirects(data.consumeBoolean());
    request.maxBodySize(data.consumeInt(0, 1_000_000));
    request.timeout(data.consumeInt(0, 5_000));
    request.userAgent(data.consumeString(40));

    try {
      HttpConnection.Response res = HttpConnection.Response.execute(request, null);
      if (data.consumeBoolean()) {
        try {
          res.parse();
        } catch (IOException ignored) {
        }
      } else {
        res.body();
      }
    } catch (IOException | IllegalArgumentException e) {
      // ignore
    }
  }

  private static final class FuzzURLStreamHandler extends URLStreamHandler {
    private final byte[] body;
    private final String[] headerKeys;
    private final String[] headerVals;
    private final int responseCode;

    FuzzURLStreamHandler(byte[] body, String[] headerKeys, String[] headerVals, int responseCode) {
      this.body = body;
      this.headerKeys = headerKeys;
      this.headerVals = headerVals;
      this.responseCode = responseCode;
    }

    @Override
    protected URLConnection openConnection(URL u) {
      return new FuzzHttpURLConnection(u, body, headerKeys, headerVals, responseCode);
    }
  }

  private static final class FuzzHttpURLConnection extends HttpURLConnection {
    private final byte[] body;
    private final String[] headerKeys;
    private final String[] headerVals;
    private final int responseCode;

    protected FuzzHttpURLConnection(URL url, byte[] body, String[] headerKeys,
        String[] headerVals, int responseCode) {
      super(url);
      this.body = body;
      this.headerKeys = headerKeys;
      this.headerVals = headerVals;
      this.responseCode = responseCode;
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
    public InputStream getErrorStream() {
      return new ByteArrayInputStream(body);
    }

    @Override
    public OutputStream getOutputStream() {
      return new ByteArrayOutputStream();
    }

    @Override
    public int getResponseCode() {
      return responseCode;
    }

    @Override
    public String getResponseMessage() {
      return "";
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

  private static byte[] gzip(byte[] input) throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    try (GZIPOutputStream gz = new GZIPOutputStream(out)) {
      gz.write(input);
    }
    return out.toByteArray();
  }

  private static byte[] deflate(byte[] input) throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    try (DeflaterOutputStream df = new DeflaterOutputStream(out)) {
      df.write(input);
    }
    return out.toByteArray();
  }
}
