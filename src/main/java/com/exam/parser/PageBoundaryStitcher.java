package com.exam.parser;

import com.exam.parser.config.ParserConfig;
import com.exam.parser.model.ParseError;
import com.exam.parser.model.ParseErrorCode;
import com.exam.parser.model.RawPage;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Stage 2: Detects rows split across page boundaries and stitches them.
 *
 * Strict conditions (Gap #3 fix):
 * - Line N has register AND missing department
 * - Line N+1 has NO register
 * - Line N+1 contains valid department alias
 * - Line N+1 has < 5 tokens (prevents merging random header lines)
 */
public class PageBoundaryStitcher {

    private final ParserConfig config;

    public PageBoundaryStitcher(ParserConfig config) {
        this.config = config;
    }

    public StitchOutput stitch(List<RawPage> pages) {
        List<RawPage> result = new ArrayList<>();
        List<ParseError> warnings = new ArrayList<>();

        for (int i = 0; i < pages.size(); i++) {
            RawPage currentPage = pages.get(i);
            String currentText = currentPage.text();

            if (i + 1 < pages.size()) {
                RawPage nextPage = pages.get(i + 1);
                String[] currentLines = currentText.split("\n");
                String[] nextLines = nextPage.text().split("\n");

                String lastLine = findLastNonEmpty(currentLines);
                String firstLine = findFirstNonEmpty(nextLines);

                if (lastLine != null && firstLine != null
                        && hasRegister(lastLine) && !hasDepartment(lastLine)
                        && !hasRegister(firstLine) && hasDepartment(firstLine)
                        && tokenCount(firstLine) < 5) {

                    // Stitch: append first line of next page to last line of current page
                    String stitchedLine = lastLine.trim() + " " + firstLine.trim();
                    currentText = replaceLast(currentText, lastLine, stitchedLine);

                    // Remove stitched line from next page
                    String nextText = removeFirst(nextPage.text(), firstLine);
                    pages.set(i + 1, new RawPage(nextPage.pageNumber(), nextText));

                    warnings.add(new ParseError(
                            currentPage.pageNumber(), -1,
                            ParseErrorCode.PAGE_BOUNDARY_STITCH, "WARNING",
                            "Stitched row across page boundary: " + stitchedLine,
                            stitchedLine
                    ));
                }
            }

            result.add(new RawPage(currentPage.pageNumber(), currentText));
        }

        return new StitchOutput(result, warnings);
    }

    private boolean hasRegister(String line) {
        String[] tokens = line.trim().split("\\s+");
        for (String token : tokens) {
            String upper = token.toUpperCase();
            for (var pattern : config.getCompiledRegisterPatterns()) {
                if (pattern.matcher(upper).matches()) return true;
            }
        }
        return false;
    }

    private boolean hasDepartment(String line) {
        String[] tokens = line.trim().split("\\s+");
        var aliasMap = config.getFlatAliasMap();
        for (String token : tokens) {
            if (aliasMap.containsKey(token.toUpperCase())) return true;
        }
        // Sliding window: pairs
        for (int i = 0; i + 1 < tokens.length; i++) {
            String pair = (tokens[i] + " " + tokens[i + 1]).toUpperCase();
            if (aliasMap.containsKey(pair)) return true;
        }
        return false;
    }

    private int tokenCount(String line) {
        return line.trim().split("\\s+").length;
    }

    private String findLastNonEmpty(String[] lines) {
        for (int i = lines.length - 1; i >= 0; i--) {
            if (!lines[i].trim().isEmpty()) return lines[i];
        }
        return null;
    }

    private String findFirstNonEmpty(String[] lines) {
        for (String line : lines) {
            if (!line.trim().isEmpty()) return line;
        }
        return null;
    }

    private String replaceLast(String text, String target, String replacement) {
        int idx = text.lastIndexOf(target);
        if (idx < 0) return text;
        return text.substring(0, idx) + replacement + text.substring(idx + target.length());
    }

    private String removeFirst(String text, String target) {
        int idx = text.indexOf(target);
        if (idx < 0) return text;
        return text.substring(0, idx) + text.substring(idx + target.length());
    }

    public record StitchOutput(List<RawPage> pages, List<ParseError> warnings) {}
}
