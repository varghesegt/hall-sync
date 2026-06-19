package com.exam.parser;

import com.exam.parser.config.ParserConfig;
import com.exam.parser.model.ParseError;
import com.exam.parser.model.ParseErrorCode;
import com.exam.parser.model.StudentRow;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Stage 5: Pattern-based row extraction.
 *
 * Per line:
 * 1. Split tokens → apply merge → scan for register (full-token match only)
 * 2. If 0 register matches → noise, skip
 * 3. If 2+ register matches → AMBIGUOUS_REGISTER (FATAL)
 * 4. Scan remaining tokens for department via sliding window (1→2→3 token widths)
 * 5. Remaining tokens → studentName
 */
public class PatternRowExtractor {

    private final ParserConfig config;
    private final TextNormalizer normalizer;

    public PatternRowExtractor(ParserConfig config, TextNormalizer normalizer) {
        this.config = config;
        this.normalizer = normalizer;
    }

    private static final Logger logger = LoggerFactory.getLogger(PatternRowExtractor.class);

    /**
     * Extracts all students from a line. Supports both Grid (multi-register) 
     * and Tabular (Name + Register) formats.
     */
    public List<StudentRow> extractAll(String line, int pageNumber, int lineNumber, String subjectName, String subjectCode, String semester, String regulation) {
        List<ParseError> errors = new ArrayList<>();
        
        // Split by whitespace
        String[] parts = line.trim().split("\\s+");
        List<String> tokens = new ArrayList<>();
        for (String p : parts) if (!p.isEmpty()) tokens.add(p);

        // --- Step 1: Find all register numbers in the line ---
        List<Pattern> registerPatterns = config.getCompiledRegisterPatterns();
        
        List<Integer> registerIndices = new ArrayList<>();
        List<String> foundRegisters = new ArrayList<>();

        for (int i = 0; i < tokens.size(); i++) {
            String token = tokens.get(i).toUpperCase();
            boolean matched = false;
            for (Pattern p : registerPatterns) {
                var matcher = p.matcher(token);
                if (matcher.matches()) {
                    registerIndices.add(i);
                    foundRegisters.add(token);
                    matched = true;
                    break;
                } else if (matcher.find()) {
                    registerIndices.add(i);
                    foundRegisters.add(matcher.group());
                    matched = true;
                    break;
                }
            }
        }

        if (foundRegisters.isEmpty()) {
            return List.of(); // Noise line
        }

        // --- Step 2: Handle extraction based on count ---
        List<StudentRow> students = new ArrayList<>();

        if (foundRegisters.size() > 1) {
            // GRID FORMAT
            for (String reg : foundRegisters) {
                String dept = resolveDepartment(reg);
                String className = dept; // As requested, Class = derived Department
                students.add(StudentRow.resolved(reg, reg, dept, className, subjectName, subjectCode, semester, regulation, pageNumber, lineNumber));
            }
        } else {
            // TABULAR FORMAT
            String reg = foundRegisters.get(0);
            int regIdx = registerIndices.get(0);
            
            List<String> nameTokens = new ArrayList<>();
            for (int i = 0; i < tokens.size(); i++) {
                if (i != regIdx) {
                    if (!isExplicitDept(tokens.get(i))) {
                        nameTokens.add(tokens.get(i));
                    }
                }
            }
            String name = String.join(" ", nameTokens).trim();
            if (name.isEmpty()) name = reg;
            
            String dept = resolveDepartment(reg);
            String className = dept;
            students.add(StudentRow.resolved(reg, name, dept, className, subjectName, subjectCode, semester, regulation, pageNumber, lineNumber));
        }

        return students;
    }

    private String resolveDepartment(String regNo) {
        if (regNo == null || regNo.length() < 10) return "UNKNOWN";
        
        if (regNo.matches("[0-9]{12}")) {
            String code = regNo.substring(6, 9);
            return switch (code) {
                case "103" -> "CIVIL";
                case "104" -> "CSE";
                case "105" -> "EEE";
                case "106" -> "ECE";
                case "114" -> "MECH";
                case "205" -> "IT";
                case "243" -> "AIDS";
                case "244" -> "AIML";
                case "631", "403" -> "MBA";
                default -> "OLD_BATCH";
            };
        }

        char degreeType = regNo.charAt(4); // P or U
        String code = regNo.substring(7, 9).toUpperCase();
        
        if (degreeType == 'P') {
            return switch (code) {
                case "PS" -> "ME(PSE)";
                case "ED" -> "ME(ED)";
                case "CM" -> "ME(CM)";
                case "CS" -> "ME(CSE)";
                case "MB" -> "MBA";
                default -> "ME(" + code + ")";
            };
        }
        
        return switch (code) {
            case "CS" -> "CSE";
            case "CB" -> "CSBS";
            case "EC" -> "ECE";
            case "ME" -> "MECH";
            case "EE" -> "EEE";
            case "MB" -> "MBA";
            case "AD" -> "AIDS";
            case "AM" -> "AIML";
            case "IT" -> "IT";
            default -> code;
        };
    }

    private boolean isExplicitDept(String token) {
        String u = token.toUpperCase();
        return u.matches("^(CSE|ECE|MECH|EEE|MBA|AIDS|AIML|IT|CIVIL|DEPARTMENT|DEPT)$") 
               || u.startsWith("(") && u.endsWith(")");
    }

    public static class ExtractionResult {
        // Kept for backward compatibility if needed, but we prefer List<StudentRow> now
        private final StudentRow row;
        private final List<ParseError> errors;
        private final boolean skipped;

        public ExtractionResult(StudentRow row, List<ParseError> errors) {
            this.row = row;
            this.errors = errors;
            this.skipped = false;
        }

        private ExtractionResult(boolean skipped) {
            this.row = null;
            this.errors = List.of();
            this.skipped = true;
        }

        public static ExtractionResult skipped() { return new ExtractionResult(true); }
        public StudentRow getRow() { return row; }
        public List<ParseError> getErrors() { return errors; }
        public boolean isSkipped() { return skipped; }
    }
}
