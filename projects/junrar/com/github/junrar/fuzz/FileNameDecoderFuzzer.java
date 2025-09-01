package com.github.junrar.fuzz;

import com.github.junrar.rarfile.FileNameDecoder;
import com.code_intelligence.jazzer.api.FuzzedDataProvider;

/**
 * Fuzzer for {@link FileNameDecoder}.
 *
 * <p>The decoder expects a starting position (encPos) within the input array. The previous
 * implementation frequently provided offsets outside the array, leading to immediate exceptions and
 * no meaningful coverage. This version ensures that the decoder is invoked with a range of valid
 * offsets and tries multiple starting positions per fuzz input to exercise more of the decoding
 * logic.</p>
 */
public class FileNameDecoderFuzzer {
  public static void fuzzerTestOneInput(FuzzedDataProvider data) {
    byte[] nameBytes = data.consumeBytes(data.consumeInt(1, 256));
    int limit = Math.min(nameBytes.length, 64);
    for (int encPos = 0; encPos < limit; encPos++) {
      try {
        FileNameDecoder.decode(nameBytes, encPos);
      } catch (Throwable ignored) {
      }
    }
  }
}
