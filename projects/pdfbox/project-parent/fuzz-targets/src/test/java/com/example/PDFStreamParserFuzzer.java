package com.example;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSStream;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdfparser.PDFStreamParser;

import java.util.List;

public class PDFStreamParserFuzzer {
  public static void fuzzerTestOneInput(FuzzedDataProvider data) throws Exception {
    byte[] streamBytes = data.consumeRemainingAsBytes();
    if (streamBytes.length == 0) return;

    // Minimal doc with one page and a crafted content stream.
    try (PDDocument doc = new PDDocument()) {
      PDPage page = new PDPage();
      doc.addPage(page);

      COSStream cosStream = new COSStream();
      cosStream.setItem("Length", new COSArray()); // provoke edge cases around Length handling
      cosStream.setItem(COSName.FILTER, new COSArray()); // empty filters array
      cosStream.createOutputStream().write(streamBytes);

      PDFStreamParser parser = new PDFStreamParser(streamBytes);
      List<Object> tokens = parser.parse(); // exercises inline images, operators, operands
      if (!tokens.isEmpty()) {
        tokens.get(0); // force object creation paths
      }
    }
  }
}
