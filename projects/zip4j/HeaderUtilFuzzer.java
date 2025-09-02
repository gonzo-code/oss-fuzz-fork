import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.FileHeader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class HeaderUtilFuzzer {
  public static void fuzzerInitialize() {
    // Initializing objects for fuzzing
  }

  public static void fuzzerTearDown() {
    // Tear down objects after fuzzing
  }

  public static void fuzzerTestOneInput(FuzzedDataProvider data) {
    List<String> names = new ArrayList<>();
    List<Integer> variantCounts = new ArrayList<>();
    int nameCount = data.consumeInt(1, 5);
    for (int i = 0; i < nameCount; i++) {
      names.add(data.consumeString(100));
      variantCounts.add(data.consumeInt(1, 3));
    }

    try {
      byte[] zipBytes = data.consumeRemainingAsBytes();
      File tempFile = File.createTempFile("tempZip", ".zip");
      try (FileOutputStream fos = new FileOutputStream(tempFile)) {
        fos.write(zipBytes);
      }

      ZipFile zipFile = new ZipFile(tempFile);

      for (int i = 0; i < names.size(); i++) {
        String baseName = names.get(i);
        int loops = variantCounts.get(i);
        for (int j = 0; j < loops; j++) {
          String candidate = baseName + j;
          for (int k = 0; k < 2; k++) {
            String attempt = k == 0 ? candidate : candidate.toUpperCase();
            FileHeader header;
            try {
              header = zipFile.getFileHeader(attempt);
            } catch (ZipException e) {
              throw new RuntimeException(e);
            }
            if (header != null) {
              header.isDirectory();
            }
          }
        }
      }

      tempFile.delete();
    } catch (IOException e) {
      // Ignore
    }
  }
}
