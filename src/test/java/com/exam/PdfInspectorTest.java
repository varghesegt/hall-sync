package com.exam;

import com.exam.parser.PdfTextExtractor;
import com.exam.parser.config.ParserConfig;
import com.exam.parser.model.RawPage;
import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Paths;

public class PdfInspectorTest {
    
    @org.junit.jupiter.api.Disabled("Missing test file allocation.pdf")
    @Test
    public void testInspectPdf() throws Exception {
        byte[] pdfBytes = Files.readAllBytes(Paths.get("allocation.pdf"));
        ParserConfig config = new ParserConfig();
        config.setTempDir("./data/parser-tmp");
        PdfTextExtractor extractor = new PdfTextExtractor(config);
        
        PdfTextExtractor.ExtractionOutput output = extractor.extract(pdfBytes);
        System.out.println("HASH: " + output.hash());
        for (RawPage page : output.pages()) {
            System.out.println("--- PAGE " + page.pageNumber() + " ---");
            System.out.println(page.text());
        }
    }
}
