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
    // Determine a fixed chunk size from the fuzzer input.
    int chunkSize = data.consumeInt(1, 32);
    String input = data.consumeRemainingAsString();

    StreamParser sp = new StreamParser(Parser.htmlParser());
    String baseUri = "";

    for (int i = 0; i < input.length(); i += chunkSize) {
      int end = Math.min(i + chunkSize, input.length());
      sp.parse(input.substring(i, end), baseUri);
    }
    sp.finish();

  }
}
