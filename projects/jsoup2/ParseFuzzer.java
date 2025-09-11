package org.jsoup.fuzz;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.helper.W3CDom;
import org.jsoup.helper.DataUtil;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class ParseFuzzer {
  public static void fuzzerTestOneInput(FuzzedDataProvider data) {
    try {
      // Raw string input
      String html = data.consumeRemainingAsString();

      // 1. Standard HTML parsing
      Document doc = Jsoup.parse(html);

      // 2. Fragment parsing (different tree-builder path)
      Jsoup.parseBodyFragment(html);

      // 3. Encoding-sensitive path (DataUtil)
      byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
      DataUtil.load(
          new ByteArrayInputStream(bytes),
          "UTF-8", // charset
          null // base URI
      );

      // 4. URL handling path using a base URI
      try {
        String base = "http://example.com/";
        URL url = new URL(base + html.replaceAll("[^a-zA-Z0-9]", ""));
        // Parsing with a user-provided base URL exercises URL normalization without
        // accessing internal helper classes.
        Jsoup.parse(html, url.toString());
      } catch (Exception ignored) {}

      // 5. W3C DOM conversion (deeper tree traversal)
      W3CDom w3c = new W3CDom();
      w3c.fromJsoup(doc);

    } catch (IllegalArgumentException | IOException ignored) {
      // Expected on malformed input
    } catch (Throwable t) {
      // Crash of interest
      throw t;
    }
  }
}
