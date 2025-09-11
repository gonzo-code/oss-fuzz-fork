package org.jsoup.fuzz;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.jsoup.helper.DataUtil;
import org.jsoup.nodes.Document;

public class DataUtilFuzzer {
  public static void fuzzerTestOneInput(FuzzedDataProvider data) throws Throwable {
    byte[] bytes = data.consumeRemainingAsBytes();
    String baseUri = data.consumeString(1000);
    String charset = data.consumeString(40);
    try (InputStream in = new ByteArrayInputStream(bytes)) {
      Document doc = DataUtil.load(in, baseUri, charset);
      if (doc != null) {
        doc.outerHtml();
      }
    } catch (IllegalArgumentException | IOException e) {
      // Known exception types.
    } catch (Throwable t) {
      throw t;
    }
  }
}
