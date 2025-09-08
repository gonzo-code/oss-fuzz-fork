import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.parser.Parser;

import java.io.StringReader;
import java.util.List;

public class FuzzParseFragment {
  private static final String[] CONTEXTS = {"div", "table", "template", "svg", "math", "button", "li"};

  public static void fuzzerTestOneInput(FuzzedDataProvider data) {
    String s = data.consumeRemainingAsString();
    String contextName = CONTEXTS[data.consumeInt(0, CONTEXTS.length - 1)];
    Element context = Jsoup.parse("<" + contextName + ">").selectFirst(contextName);

    Parser parser = data.consumeBoolean() ? Parser.xmlParser().newInstance() : Parser.htmlParser().newInstance();
    if (parser.getTreeBuilder().defaultNamespace().equals(Parser.NamespaceXml)) {
      // Fuzz namespace prefixes / illegal name chars when using XML parser
      if (data.consumeBoolean()) {
        s = data.consumeString(5) + ":" + s;
      }
      if (data.consumeBoolean()) {
        s = s.replace('a', ':');
      }
    }

    try {
      List<Node> nodes = Parser.parseFragmentInput(new StringReader(s), context, "");
      Document doc = Document.createShell("");
      for (Node n : nodes) {
        doc.body().appendChild(n);
      }
      roundTrip(doc);
    } catch (IllegalArgumentException | IllegalStateException ignored) {
    }
  }

  private static void roundTrip(Document doc) {
    if (doc == null) return;
    String outer = doc.outerHtml();
    try {
      Document parsed = Jsoup.parse(outer);
      int c1 = doc.getAllElements().size();
      int c2 = parsed.getAllElements().size();
      if (c2 > c1 * 10) {
        // soft check
      }
    } catch (IllegalArgumentException | IllegalStateException ignored) {
    }
  }
}
