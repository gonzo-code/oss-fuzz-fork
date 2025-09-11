package org.jsoup.fuzz;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.jsoup.helper.DataUtil;
import org.jsoup.nodes.Document;

// Dictionary
// Use this with the libFuzzer -dict=DICT.file flag
//
// Fuzzer function priority
// Use one of these functions as input to libFuzzer with flag -focus_function name
// -focus_function=org.jsoup.internal.StringUtil.borrowBuilder
public class DataUtilFuzzer {
    private static final String[] CHARSETS = {
        "UTF-8", "UTF-16", "UTF-16LE", "UTF-16BE", "UTF-32",
        "ISO-8859-1", "windows-1252", "Shift_JIS", "KOI8-R", "Big5", null
    };

    public static void fuzzerTestOneInput(FuzzedDataProvider data) throws Throwable {
      String baseUri = "http://" + data.consumeAsciiString(20);
      if (baseUri.equals("http://")) {
        baseUri += "example.com/";
      }
      String charset = data.pickValue(CHARSETS);
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
