package com.exam.parser;

import com.exam.parser.config.ParserConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Stage 3: Unicode cleanup + safe adjacent-token merge for split register numbers.
 *
 * Token merge rule (Gap #1 fix — anti over-merge):
 * Only merge adjacent tokens if:
 * - Combined string matches a register regex
 * - At least one of the tokens starts with a digit
 */
public class TextNormalizer {

    private final ParserConfig config;

    public TextNormalizer(ParserConfig config) {
        this.config = config;
    }

    /**
     * Normalizes raw page text into clean lines.
     */
    public List<String> normalize(String rawText) {
        // Step 1: Unify line breaks
        String text = rawText.replace("\r\n", "\n").replace("\r", "\n");

        // Step 2: Non-breaking space → regular space
        text = text.replace("\u00A0", " ");

        // Step 3: Zero-width chars
        text = text.replace("\u200B", "").replace("\uFEFF", "");

        // Step 4-6: Per-line processing
        String[] rawLines = text.split("\n");
        List<String> result = new ArrayList<>();

        for (String line : rawLines) {
            // Step 4: Collapse multiple spaces
            String cleaned = line.replaceAll(" {2,}", "  "); // preserve double-space as column delimiter
            // Step 5: Trim
            cleaned = cleaned.trim();
            // Step 6: Skip blank
            if (!cleaned.isEmpty()) {
                result.add(cleaned);
            }
        }

        return result;
    }

    /**
     * Applies safe adjacent-token merging for split register numbers.
     * Only merges if the combined token matches a register pattern
     * AND at least one token starts with a digit.
     */
    public List<String> mergeTokens(List<String> tokens) {
        List<String> merged = new ArrayList<>(tokens);
        List<Pattern> patterns = config.getCompiledRegisterPatterns();

        // Try pairs first
        for (int i = 0; i < merged.size() - 1; i++) {
            String a = merged.get(i);
            String b = merged.get(i + 1);
            String combined = (a + b).toUpperCase();

            // Gap #1 guard: at least one token must start with digit
            boolean hasDigitStart = Character.isDigit(a.charAt(0)) || Character.isDigit(b.charAt(0));

            if (hasDigitStart && matchesAnyPattern(combined, patterns)) {
                merged.set(i, combined);
                merged.remove(i + 1);
                break; // only one merge per line (safe)
            }
        }

        // Try triples (rare, but possible: "21" + "CSE" + "101")
        for (int i = 0; i < merged.size() - 2; i++) {
            String a = merged.get(i);
            String b = merged.get(i + 1);
            String c = merged.get(i + 2);
            String combined = (a + b + c).toUpperCase();

            boolean hasDigitStart = Character.isDigit(a.charAt(0));

            if (hasDigitStart && matchesAnyPattern(combined, patterns)) {
                merged.set(i, combined);
                merged.remove(i + 2);
                merged.remove(i + 1);
                break;
            }
        }

        return merged;
    }

    private boolean matchesAnyPattern(String token, List<Pattern> patterns) {
        for (Pattern p : patterns) {
            if (p.matcher(token).matches()) return true;
        }
        return false;
    }
}
