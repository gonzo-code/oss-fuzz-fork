package org.jsoup.helper;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.jsoup.helper.HttpConnection;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Fuzzes {@link UrlBuilder}, which normalizes and encodes URLs.
 */
public class UrlBuilderFuzzer {
  private static final int MAX_URL_LENGTH = 2048;

  public static void fuzzerTestOneInput(FuzzedDataProvider data) {
    String rawUrl = data.consumeString(MAX_URL_LENGTH);
    try {
      URL url = new URL(rawUrl);
      UrlBuilder builder = new UrlBuilder(url);
      if (data.consumeBoolean()) {
        HttpConnection.KeyVal kv = HttpConnection.KeyVal.create(
            data.consumeString(32), data.consumeString(32));
        builder.appendKeyVal(kv);
      }
      builder.build();
    } catch (MalformedURLException | UnsupportedEncodingException | IllegalArgumentException e) {
      // Ignore expected failures from invalid input or encoding issues.
    }
  }
}
