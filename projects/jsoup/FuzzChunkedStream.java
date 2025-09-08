import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

public class FuzzChunkedStream {
  private static class ChunkedInputStream extends InputStream {
    private final InputStream in;
    private final FuzzedDataProvider data;
    private int remaining;

    ChunkedInputStream(byte[] bytes, FuzzedDataProvider data) {
      this.in = new ByteArrayInputStream(bytes);
      this.data = data;
      this.remaining = 0;
    }

    @Override
    public int read(byte[] b, int off, int len) {
      if (remaining == 0) {
        remaining = 1 + data.consumeInt(0, 8191);
      }
      int toRead = Math.min(len, remaining);
      int r = in.read(b, off, toRead);
      if (r > 0) remaining -= r;
      return r;
    }

    @Override
    public int read() {
      byte[] one = new byte[1];
      int r = read(one, 0, 1);
      return r == -1 ? -1 : one[0] & 0xff;
    }
  }

  private static final String[] CONTEXTS = {"div", "table", "svg", "math"};

  public static void fuzzerTestOneInput(FuzzedDataProvider data) {
    byte[] input = data.consumeRemainingAsBytes();
    // Optionally inject prefix/suffix for structure
    if (data.consumeInt(0,9) < 2) {
      byte[] prefix = "<svg><script></script></svg>".getBytes(StandardCharsets.UTF_8);
      byte[] combined = new byte[prefix.length + input.length];
      System.arraycopy(prefix,0,combined,0,prefix.length);
      System.arraycopy(input,0,combined,prefix.length,input.length);
      input = combined;
    }
    if (data.consumeInt(0,9) < 2) {
      byte[] suffix = "<table><tr><td></td></tr></table>".getBytes(StandardCharsets.UTF_8);
      byte[] combined = new byte[input.length + suffix.length];
      System.arraycopy(input,0,combined,0,input.length);
      System.arraycopy(suffix,0,combined,input.length,suffix.length);
      input = combined;
    }

    InputStream in = new ChunkedInputStream(input, data);
    try {
      if (data.consumeBoolean()) {
        Document doc = Jsoup.parse(in, "UTF-8", "");
        doc.outerHtml();
      } else {
        Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8);
        String contextName = CONTEXTS[data.consumeInt(0, CONTEXTS.length - 1)];
        Element context = Jsoup.parse("<" + contextName + ">").selectFirst(contextName);
        Parser.htmlParser().newInstance().parseFragmentInput(reader, context, "");
      }
    } catch (IllegalArgumentException | IllegalStateException ignored) {
    } catch (Exception e) {
      // rethrow other exceptions
      throw new RuntimeException(e);
    }
  }
}
