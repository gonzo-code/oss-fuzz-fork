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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.jsoup.parser.XmlTreeBuilder;

public class XmlFuzzer {
  public static void fuzzerTestOneInput(FuzzedDataProvider data) throws Exception {
    String input = data.consumeRemainingAsString();
    XmlTreeBuilder treeBuilder = new XmlTreeBuilder();

    Method parse = XmlTreeBuilder.class.getDeclaredMethod("parse", String.class, String.class);
    parse.setAccessible(true);
    try {
      parse.invoke(treeBuilder, input, "");
    } catch (InvocationTargetException e) {
      Throwable cause = e.getCause();
      if (cause instanceof Exception) {
        throw (Exception) cause;
      }
      throw new RuntimeException(cause);
    }
  }
}
