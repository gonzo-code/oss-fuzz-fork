package com.github.junrar.fuzz;

import com.github.junrar.rarfile.FileNameDecoder;
import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FileNameDecoderFuzzer {
  public static void fuzzerTestOneInput(FuzzedDataProvider data) {
    byte[] nameBytes = data.consumeBytes(data.consumeInt(0, 256));
    int encFlags = data.consumeInt(0, 0xFFFF);
    try {
      FileNameDecoder.decode(nameBytes, encFlags);
    } catch (Throwable ignored) {
    }
  }
}
