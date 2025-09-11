import com.code_intelligence.jazzer.api.FuzzedDataProvider;

import org.jsoup.Jsoup;

public class OGHtmlFuzzer {
  public static void fuzzerTestOneInput(FuzzedDataProvider data) {
    Jsoup.parse(data.consumeRemainingAsString());
  }
}
