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

import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;

/**
 * Fuzzer for exercising the jsoup XML parser.
 *
 * <p>The original fuzzer used reflection to bypass jsoup's internal locking
 * which in turn caused fuzz-introspector to report blocking operations and
 * limited the throughput.  This version relies only on the public API and
 * bounds the size of the consumed input to keep parsing times short.</p>
 */
public class XmlFuzzer {
  private static final int MAX_INPUT_SIZE = 4_096; // avoid pathological inputs

  public static void fuzzerTestOneInput(FuzzedDataProvider data) {
    String xml = data.consumeString(MAX_INPUT_SIZE);

    Parser parser = Parser.xmlParser();

    try {
      parser.parseInput(xml, "");
    } catch (IllegalArgumentException | IllegalStateException ignored) {
      // Expected for malformed input.
    }

    try {
      parser.parseFragmentInput(xml, new Element("root"), "");
    } catch (IllegalArgumentException | IllegalStateException ignored) {
      // Expected for malformed input.
    }
  }
}
