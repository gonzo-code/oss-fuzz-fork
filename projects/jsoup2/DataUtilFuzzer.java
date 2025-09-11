package org.jsoup.fuzz;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.jsoup.helper.DataUtil;
import org.jsoup.internal.StringUtil;

import org.jsoup.nodes.Document;

public class DataUtilFuzzer {
    private static final String[] CHARSETS = {
        "UTF-8", "UTF-16", "UTF-16LE", "UTF-16BE", "UTF-32",
        "ISO-8859-1", "windows-1252", "Shift_JIS", "KOI8-R", "Big5", null
    };

    public static void fuzzerTestOneInput(FuzzedDataProvider data) throws Throwable {
      // Exercise StringUtil URL resolution with arbitrary relative paths.
      String relUrl = data.consumeString(100);
      String baseUri = StringUtil.resolve("http://example.com/", relUrl);
      String charset = data.pickValue(CHARSETS);

      // Consume additional strings to drive various StringUtil helpers.
      String random = data.consumeString(100);
      String sep = data.consumeAsciiString(1);
      try {
        StringUtil.join(new String[] {random, relUrl}, sep);
        StringUtil.normaliseWhitespace(random);
        StringUtil.padding(data.consumeInt(-1, 40));
        StringUtil.isAscii(random);
        StringUtil.isNumeric(random);
        StringUtil.isBlank(random);
      } catch (IllegalArgumentException ignored) {
        // Expected for invalid arguments in StringUtil.
      }

      byte[] bytes = data.consumeRemainingAsBytes();
      try (InputStream in = new ByteArrayInputStream(bytes)) {
        Document doc = DataUtil.load(in, baseUri, charset);
        if (doc != null) {
          doc.outerHtml();
        }
      } catch (IllegalArgumentException | IOException e) {
        // Known exception types.
      }
    }
  }

