// Copyright 2024 Google LLC
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

import org.jsoup.parser.Parser;
import org.jsoup.parser.StreamParser;

import java.io.IOException;

public class StreamParserFuzzer {
  public static void fuzzerTestOneInput(FuzzedDataProvider data) {
    Parser parser = Parser.htmlParser();
    // Enable additional parser features to increase code coverage.
    parser.setTrackErrors(data.consumeInt(0, 100));
    parser.setTrackPosition(data.consumeBoolean());

    StreamParser sp = new StreamParser(parser);

    // Choose whether unescapeEntities should run in attribute mode.
    boolean inAttribute = data.consumeBoolean();

    // Feed the input to the StreamParser in several chunks to exercise the
    // streaming code paths and reduce locking contention.
    StringBuilder allInput = new StringBuilder();
    String baseUri = "";
    int chunks = 1;
    if (data.remainingBytes() > 0) {
      chunks = data.consumeInt(1, Math.min(4, data.remainingBytes()));
    }
    for (int i = 0; i < chunks && data.remainingBytes() > 0; i++) {
      int len = 0;
      if (data.remainingBytes() > 0) {
        len = data.consumeInt(0, data.remainingBytes());
      }
      String chunk = data.consumeString(len);
      allInput.append(chunk);
      sp.parse(chunk, baseUri);
    }
    String rest = data.consumeRemainingAsString();
    allInput.append(rest);
    sp.parse(rest, baseUri);
    try {
      sp.complete();
    } catch (IOException ignored) {
      // Ignore I/O errors from the parser.
    }
    sp.close();

    // Exercise entity unescaping with guaranteed '&' to avoid early return.
    Parser.unescapeEntities(allInput.append('&').toString(), inAttribute);
  }
}
