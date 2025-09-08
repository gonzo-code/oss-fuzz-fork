import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.parser.Parser;
import org.jsoup.parser.Tag;
import org.jsoup.parser.TagSet;

public class FuzzParseHtml {
  public static void fuzzerTestOneInput(FuzzedDataProvider data) {
    String s = data.consumeRemainingAsString();
    // Occasionally inject small foreign content islands
    if (data.consumeBoolean()) {
      s += "<svg><script></script><circle/></svg>";
    }
    if (data.consumeBoolean()) {
      s += "<math><mi>x</mi></math>";
    }
    if (data.consumeBoolean()) {
      s += "<b><i></b></i>";
    }
    Parser p = Parser.newInstance();
    TagSet tagSet = TagSet.Html();

    String[] tags = {
        "div", "span", "p", "b", "i", "custom", "foreign", "svg", "math",
        "table", "tr", "td"};
    for (String name : tags) {
      String ns = Parser.NamespaceHtml;
      if (name.equals("svg")) ns = Parser.NamespaceSvg;
      else if (name.equals("math")) ns = Parser.NamespaceMathml;
      Tag tag = tagSet.valueOf(name, ns);
      if (data.consumeBoolean()) tag.set(Tag.SelfClose);
      if (data.consumeBoolean()) tag.set(Tag.PreserveWhitespace);
      if (data.consumeBoolean()) {
        tag.clear(Tag.Data);
        tag.clear(Tag.RcData);
        if (data.consumeBoolean()) tag.set(Tag.Data);
        else tag.set(Tag.RcData);
      }
    }
    // Random custom tag
    String custom = data.consumeString(10);
    if (!custom.isEmpty()) {
      Tag tag = tagSet.valueOf(custom, Parser.NamespaceHtml);
      if (data.consumeBoolean()) tag.set(Tag.SelfClose);
    }
    p.tagSet(tagSet);

    try {
      Document doc = Jsoup.parse(s, "", p);
      roundTrip(doc);
    } catch (IllegalArgumentException | IllegalStateException ignored) {
    }

    try {
      Document body = Parser.parseBodyFragment(s, "");
      roundTrip(body);
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
        // soft check; do nothing
      }
    } catch (IllegalArgumentException | IllegalStateException ignored) {
    }
  }
}
