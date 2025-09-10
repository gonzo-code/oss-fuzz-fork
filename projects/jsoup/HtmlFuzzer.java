// Copyright 2021 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//
////////////////////////////////////////////////////////////////////////////////

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

import java.io.StringReader;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.ParseSettings;
import org.jsoup.parser.Parser;

public class HtmlFuzzer {
  private static final int MAX_INPUT_SIZE = 10_000;

  public static void fuzzerTestOneInput(FuzzedDataProvider data) {
    String baseUri = data.consumeString(1000);
    String html = data.consumeString(MAX_INPUT_SIZE);

    Parser parser = Parser.htmlParser();
    parser.setTrackErrors(data.consumeInt(0, 10));
    parser.setTrackPosition(data.consumeBoolean());
    parser.settings(new ParseSettings(data.consumeBoolean(), data.consumeBoolean()));
    Document doc = parser.parseInput(new StringReader(html), baseUri);
    if (!html.isEmpty()) {
      int start = data.consumeInt(0, html.length());
      int end = data.consumeInt(start, html.length());
      String sub = html.substring(start, end);
      boolean inAttr = data.consumeBoolean();
      parser.unescapeEntities(sub, inAttr);
      Parser.unescapeEntities(sub, inAttr);
    }
    parser.parseFragmentInput(new StringReader(html), new Element("ctx"), baseUri);
    parser.getErrors();

    Jsoup.parse(html);
    Jsoup.parseBodyFragment(html);

    for (Element element : doc.getAllElements()) {
      switch (Math.floorMod(data.consumeInt(), 3)) {

        case 0:
          element.tagName();
          break;
        case 1:
          element.tag().prefix();
          break;
        default:
          element.tag().set(data.consumeInt());
          break;
      }
    }
  }
}
