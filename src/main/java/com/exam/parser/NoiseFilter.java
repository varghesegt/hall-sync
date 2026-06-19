package com.exam.parser;

import com.exam.parser.config.ParserConfig;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Stage 4: Removes header rows, footer rows, page numbers, and structural noise.
 *
 * A line is dropped if it matches noise patterns AND does NOT contain a register token.
 * Register presence takes precedence (prevents dropping valid data rows that happen
 * to contain noisy substrings).
 */
public class NoiseFilter {

    private final ParserConfig config;

    public NoiseFilter(ParserConfig config) {
        this.config = config;
    }

    /**
     * Returns true if the line should be KEPT (is NOT noise).
     */
    public boolean isDataLine(String line) {
        // Lines with fewer than 3 tokens are almost certainly noise
        String[] tokens = line.trim().split("\\s+");
        if (tokens.length < 3) {
            // Exception: if a token matches register pattern, keep it
            if (containsRegisterToken(tokens)) return true;
            return false;
        }

        // Entirely numeric lines (page numbers)
        if (line.trim().matches("^\\d+$")) return false;

        // No alphanumeric content (separators like "---")
        if (!line.matches(".*[a-zA-Z0-9].*")) return false;

        // Check noise patterns — but register presence overrides
        List<Pattern> noisePatterns = config.getCompiledNoisePatterns();
        if (noisePatterns != null) {
            for (Pattern p : noisePatterns) {
                if (p.matcher(line.trim()).matches()) {
                    // Override: if line also contains a register, keep it
                    if (containsRegisterToken(tokens)) return true;
                    return false;
                }
            }
        }

        return true;
    }

    private boolean containsRegisterToken(String[] tokens) {
        List<Pattern> registerPatterns = config.getCompiledRegisterPatterns();
        for (String token : tokens) {
            String upper = token.toUpperCase();
            for (Pattern p : registerPatterns) {
                // Try exact match first
                if (p.matcher(upper).matches()) return true;
                // Fallback: search within token (handles joined tokens like "1.22CSE001")
                if (p.matcher(upper).find()) return true;
            }
        }
        return false;
    }
}
