package com.exam.parser;

import com.exam.parser.config.ParserConfig;
import com.exam.parser.model.ParseError;
import com.exam.parser.model.ParseResult;
import com.exam.parser.model.ParseStatus;
import com.exam.parser.model.RawPage;
import com.exam.parser.model.StudentRow;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Orchestrates the 6-stage parsing pipeline.
 * Stateless — thread-safe to singleton inject.
 */
public class PdfParserPipeline {

    private static final Logger logger = LoggerFactory.getLogger(PdfParserPipeline.class);

    private final PdfTextExtractor extractor;
    private final PageBoundaryStitcher stitcher;
    private final TextNormalizer normalizer;
    private final NoiseFilter noiseFilter;
    private final PatternRowExtractor rowExtractor;
    private final ParseValidator validator;
    private final MeterRegistry meterRegistry;

    public PdfParserPipeline(ParserConfig config, MeterRegistry meterRegistry) {
        this.extractor = new PdfTextExtractor(config);
        this.stitcher = new PageBoundaryStitcher(config);
        this.normalizer = new TextNormalizer(config);
        this.noiseFilter = new NoiseFilter(config);
        this.rowExtractor = new PatternRowExtractor(config, normalizer);
        this.validator = new ParseValidator();
        this.meterRegistry = meterRegistry;
    }

    public ParseResult parse(byte[] pdfBytes) {
        Timer.Sample sample = Timer.start(meterRegistry);

        try {
            // Stage 1: Extraction
            PdfTextExtractor.ExtractionOutput extOut = extractor.extract(pdfBytes);
            List<RawPage> pages = extOut.pages();
            String hash = extOut.hash();

            // Stage 2: Stitching
            PageBoundaryStitcher.StitchOutput stitchOut = stitcher.stitch(pages);
            pages = stitchOut.pages();
            List<ParseError> errors = new ArrayList<>(stitchOut.warnings());

            List<StudentRow> students = new ArrayList<>();
            int totalLinesScanned = 0;
            int noiseCount = 0;
            int invalidRowCount = 0;

            String currentSubject = null;
            String currentSubjectCode = null;
            String currentSemester = "UNKNOWN";
            String currentRegulation = "UNKNOWN";
            StringBuilder headerAccumulator = new StringBuilder();
            boolean isCollectingHeader = false;

            for (RawPage page : pages) {
                List<String> lines = normalizer.normalize(page.text());

                for (int lineNumber = 1; lineNumber <= lines.size(); lineNumber++) {
                    String line = lines.get(lineNumber - 1).trim();
                    totalLinesScanned++;

                    logger.debug("P[{}]:L[{}] - Processing line: {}", page.pageNumber(), lineNumber, line);

                    // 1. Detect New Header Start
                    if (line.toUpperCase().contains("COURSE NAME & CODE")) {
                        // Flush old accumulator if it exists
                        headerAccumulator.setLength(0);
                        String content = line;
                        if (line.contains(":")) {
                            content = line.split(":", 2)[1].trim();
                        }
                        headerAccumulator.append(content);
                        isCollectingHeader = true;
                        continue;
                    }

                    // 2. Metadata Extraction Logic (Priority Based Selection)
                    if (isCollectingHeader) {
                        String fullBuffer = (headerAccumulator.toString() + " " + line).trim();
                        
                        // SYLLABUS CODE MATCHING (Priority 1) - Broadened for any academic code
                        // Pattern: 1-5 letters, followed by 1-5 numbers, then optional alphanumeric suffix
                        Pattern syllabusPattern = Pattern.compile("([A-Z]{1,5}[0-9]{1,5}[A-Z0-9-]*(?:\\([A-Z0-9]+\\))?)");
                        Matcher syllabusMatcher = syllabusPattern.matcher(fullBuffer.toUpperCase());
                        
                        // BUNDLE NUMBER MATCHING (Fallback)
                        Pattern bundlePattern = Pattern.compile("BUNDLE NO\\s*(\\d{5})");
                        Matcher bundleMatcher = bundlePattern.matcher(fullBuffer.toUpperCase());
                        
                        String bestCode = null;
                        int bestCodeStart = -1;

                        // Identify the absolute best candidate for "Subject Code"
                        // We check for syllabus codes FIRST
                        if (syllabusMatcher.find()) {
                            bestCode = syllabusMatcher.group();
                            bestCodeStart = syllabusMatcher.start();
                        } else if (bundleMatcher.find()) {
                            bestCode = bundleMatcher.group(1);
                            bestCodeStart = bundleMatcher.start();
                        }

                        if (bestCode != null) {
                            // Stage A: Initial Name Capture
                            String rawSubject = fullBuffer.substring(0, bestCodeStart).trim();
                            
                            // Stage B: Greedy Cleanup (Production Step)
                            // We strip all administrative noise to leave ONLY the subject name
                            String cleanSubject = rawSubject
                                .replace("COURSE NAME & CODE", "")
                                .replace(":", "")
                                .replace("BUNDLE NO", "")
                                .replace("PAPER NAME", "")
                                .replace("THEORY", "")
                                .replace("( CUM PRACTICAL )", "")
                                .replace("( THEORY CUM PRACTICAL )", "")
                                .replaceAll("\\d{5}", "") // Remove leaked bundle numbers
                                .replaceAll("-", " ")      // Clean up punctuation
                                .replaceAll("\\s+", " ")   // Normalize whitespace
                                .toUpperCase()
                                .trim();
                            
                            // Last failsafe for trailing fragments
                            if (cleanSubject.endsWith(" AND")) cleanSubject = cleanSubject.substring(0, cleanSubject.length()-4).trim();
                            if (cleanSubject.endsWith("-")) cleanSubject = cleanSubject.substring(0, cleanSubject.length()-1).trim();

                            currentSubject = cleanSubject;
                            currentSubjectCode = bestCode.replaceAll("\\s*\\(.*?\\)", "").trim();
                            
                            // Try to find Semester/Regulation in the buffer
                            Pattern semPattern = Pattern.compile("SEMESTER\\s*[:\\-]?\\s*(\\d+)");
                            Matcher semMatcher = semPattern.matcher(fullBuffer.toUpperCase());
                            if (semMatcher.find()) currentSemester = semMatcher.group(1);

                            Pattern regPattern = Pattern.compile("REGULATION\\s*[:\\-]?\\s*(\\d+)");
                            Matcher regMatcher = regPattern.matcher(fullBuffer.toUpperCase());
                            if (regMatcher.find()) currentRegulation = regMatcher.group(1);

                            logger.info("P[{}]:L[{}] - Universal Meta-Data: {} [Code: {}] [Sem: {}] [Reg: {}]", 
                                page.pageNumber(), lineNumber, currentSubject, currentSubjectCode, currentSemester, currentRegulation);
                                
                            isCollectingHeader = false;
                            headerAccumulator.setLength(0);
                            continue;
                        }

                        // Stop collecting if we hit a student record (failsafe)
                        if (!rowExtractor.extractAll(line, page.pageNumber(), lineNumber, "DUMMY", "DUMMY", "DUMMY", "DUMMY").isEmpty()) {
                            isCollectingHeader = false;
                            headerAccumulator.setLength(0);
                        } else {
                            // Header Continuation Noise check
                            boolean isNoise = line.toUpperCase().startsWith("EXAM DATE:") || 
                                             line.toUpperCase().contains("ATTENDANCE STATUS") ||
                                             line.toUpperCase().matches("^[BM]\\.E-.*");
                            
                            if (!isNoise) {
                                headerAccumulator.append(" ").append(line);
                            } else {
                                isCollectingHeader = false;
                                headerAccumulator.setLength(0);
                            }
                        }
                        continue;
                    }

                    // 3. Noise Filter (Data Lines)
                    if (!noiseFilter.isDataLine(line)) {
                        noiseCount++;
                        continue;
                    }

                    // 4. Student Extraction
                    List<StudentRow> extracted = rowExtractor.extractAll(line, page.pageNumber(), lineNumber, currentSubject, currentSubjectCode, currentSemester, currentRegulation);
                    
                    if (extracted.isEmpty()) {
                        noiseCount++;
                    } else {
                        students.addAll(extracted);
                    }
                }
                logger.info("P[{}]: Completed. Found {} students so far.", page.pageNumber(), students.size());
            }

            // 5. Silent Deduplication (Fix for PDFs with Attendance + Summary sheets)
            java.util.Map<String, StudentRow> uniqueStudents = new java.util.LinkedHashMap<>();
            for (StudentRow s : students) {
                uniqueStudents.putIfAbsent(s.registerNumber(), s);
            }
            List<StudentRow> deduplicatedStudents = new ArrayList<>(uniqueStudents.values());

            // Stage 6: Validation
            List<ParseError> validationErrors = validator.validate(deduplicatedStudents, totalLinesScanned, noiseCount, invalidRowCount);
            errors.addAll(validationErrors);

            boolean hasFatal = errors.stream().anyMatch(e -> "FATAL".equals(e.severity()));
            boolean hasWarnings = errors.stream().anyMatch(e -> "WARNING".equals(e.severity()));

            ParseStatus status;
            if (hasFatal) {
                status = ParseStatus.FAILED;
            } else if (hasWarnings || deduplicatedStudents.stream().anyMatch(StudentRow::requiresManualResolution)) {
                status = ParseStatus.PASSED_WITH_WARNINGS;
            } else {
                status = ParseStatus.PASSED;
            }

            logger.info("Parsed PDF {} pages, {} students. Status: {}", pages.size(), deduplicatedStudents.size(), status);

            return new ParseResult(status, deduplicatedStudents, errors, hash, pages.size(), totalLinesScanned);

        } catch (IOException e) {
            logger.error("Failed to extract text from PDF", e);
            throw new RuntimeException("PDF extraction failed", e);
        } finally {
            sample.stop(meterRegistry.timer("pdf.parse.time"));
        }
    }
}
