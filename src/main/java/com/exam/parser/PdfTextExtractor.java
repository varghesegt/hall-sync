package com.exam.parser;

import com.exam.parser.config.ParserConfig;
import com.exam.parser.model.RawPage;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

/**
 * Stage 1: Disk-backed PDF text extraction with position-based sorting.
 *
 * ARCHITECTURE NOTE (future scale):
 * Currently loads all pages' text into List<RawPage> in memory.
 * For very large PDFs (500+ pages), consider switching to page-by-page
 * streaming extraction where each page is normalized/extracted/discarded
 * before the next is loaded. This would reduce peak memory from O(pages)
 * to O(1) for the text buffer.
 */
public class PdfTextExtractor {

    private final ParserConfig config;

    public PdfTextExtractor(ParserConfig config) {
        this.config = config;
    }

    /**
     * Extracts text per-page with position-based sorting (determinism anchor).
     * @return list of RawPage records + SHA-256 hash of input bytes
     */
    public ExtractionOutput extract(byte[] pdfBytes) throws IOException {
        String hash = computeSha256(pdfBytes);

        // Disk-backed parsing: isolate to dedicated temp dir to avoid /tmp pressure
        Path dir = Paths.get(config.getTempDir());
        Files.createDirectories(dir);
        File tempFile = Files.createTempFile(dir, "exam-parse-", ".pdf").toFile();
        tempFile.deleteOnExit(); // JVM crash safety net — prevents disk leak
        try {
            Files.write(tempFile.toPath(), pdfBytes);

            List<RawPage> pages = new ArrayList<>();
            try (PDDocument doc = Loader.loadPDF(tempFile)) {
                PDFTextStripper stripper = new PDFTextStripper();
                stripper.setSortByPosition(true);

                int totalPages = doc.getNumberOfPages();
                for (int i = 1; i <= totalPages; i++) {
                    stripper.setStartPage(i);
                    stripper.setEndPage(i);
                    String text = stripper.getText(doc);
                    pages.add(new RawPage(i, text));
                }
            }

            return new ExtractionOutput(pages, hash);
        } finally {
            tempFile.delete();
        }
    }

    private String computeSha256(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(data);
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    public record ExtractionOutput(List<RawPage> pages, String hash) {}
}
