package com.example;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdfparser.PDFParser;
import org.apache.pdfbox.cos.COSDocument;

import java.io.ByteArrayInputStream;

public class PDFParserFuzzer {
  public static void fuzzerTestOneInput(FuzzedDataProvider data) throws Exception {
    byte[] pdf = data.consumeRemainingAsBytes();
    if (pdf.length < 8) return; // cheap reject
    // Try parsing with incremental updates by appending noise/trailer fragments.
    byte[] tail = ("%\n" + data.consumeAsciiString(0, 64) + "\nxref\n0 1\n0000000000 65535 f \ntrailer\n<< /Size 1 >>\nstartxref\n0\n%%EOF\n").getBytes();
    byte[] buf = new byte[pdf.length + tail.length];
    System.arraycopy(pdf, 0, buf, 0, pdf.length);
    System.arraycopy(tail, 0, buf, pdf.length, tail.length);

    try (ByteArrayInputStream bais = new ByteArrayInputStream(buf)) {
      PDFParser parser = new PDFParser(bais);
      parser.parse();
      try (COSDocument cosDoc = parser.getDocument(); PDDocument doc = new PDDocument(cosDoc)) {
        // Touch page tree & metadata paths to broaden coverage.
        if (doc.getNumberOfPages() > 0) {
          doc.getPage(0).getResources();
        }
        doc.getDocumentInformation();
        doc.getDocumentCatalog();
      }
    }
  }
}
