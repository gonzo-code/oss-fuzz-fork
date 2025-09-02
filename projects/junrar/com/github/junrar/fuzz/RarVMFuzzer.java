package com.github.junrar.fuzz;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.github.junrar.unpack.vm.RarVM;
import com.github.junrar.unpack.vm.VMPreparedProgram;

public class RarVMFuzzer {
  public static void fuzzerInitialize() {
    // Initializing objects for fuzzing
  }

  public static void fuzzerTearDown() {
    // Tear down objects after fuzzing
  }

  public static void fuzzerTestOneInput(FuzzedDataProvider data) {
    int vmLoops = data.consumeInt(1, 5);
    for (int i = 0; i < vmLoops; i++) {
      RarVM vm = new RarVM();
      int prepareLoops = data.consumeInt(1, 5);
      for (int j = 0; j < prepareLoops; j++) {
        VMPreparedProgram program = new VMPreparedProgram();
        int len = data.consumeInt(0, 4096);
        byte[] codeBytes = data.consumeBytes(len);
        int codeSize = 0;
        if (codeBytes.length > 0) {
          codeSize = data.consumeInt(0, codeBytes.length);
        }
        try {
          vm.prepare(codeBytes, codeSize, program);
          int execLoops = data.consumeInt(1, 3);
          for (int k = 0; k < execLoops; k++) {
            try {
              vm.execute(program);
            } catch (Exception ignored) {
            }
          }
        } catch (Exception ignored) {
        }
      }
    }
  }
}
