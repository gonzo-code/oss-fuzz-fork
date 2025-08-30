package com.example;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.pdfbox.contentstream.PDContentStream;
import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSStream;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdfparser.PDFStreamParser;

import java.io.ByteArrayInputStream;

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
      cosStream.setFilters(new COSArray());        // empty filters array
      cosStream.createOutputStream().write(streamBytes);

      PDContentStream cs = new PDContentStream(page);
      PDFStreamParser parser = new PDFStreamParser(new ByteArrayInputStream(streamBytes));
      parser.parse(); // exercises inline images, operators, operands
      // Touch the tokens list to force object creation paths.
      if (parser.getTokens() != null && !parser.getTokens().isEmpty()) {
        parser.getTokens().get(0);
      }
    }
  }
}
