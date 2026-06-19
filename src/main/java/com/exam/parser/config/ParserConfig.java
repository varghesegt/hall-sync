package com.exam.parser.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

@Component
@ConfigurationProperties(prefix = "parser")
public class ParserConfig {

    private static final Logger logger = LoggerFactory.getLogger(ParserConfig.class);

    private int maxFileSizeMb = 10;
    private List<String> registerPatterns;
    private Map<String, List<String>> departments;
    private List<String> noisePatterns;
    private int maxTokensPerLine = 20;
    private int slidingWindowMax = 3;
    private String tempDir = System.getProperty("java.io.tmpdir") + "/exam-parser";
    private String configVersion; // computed at startup

    // --- Compiled caches (built on first access) ---
    private List<Pattern> compiledRegisterPatterns;
    private Map<String, String> flatAliasMap;
    private List<Pattern> compiledNoisePatterns;

    public int getMaxFileSizeMb() { return maxFileSizeMb; }
    public void setMaxFileSizeMb(int maxFileSizeMb) { this.maxFileSizeMb = maxFileSizeMb; }

    public List<String> getRegisterPatterns() { return registerPatterns; }
    public void setRegisterPatterns(List<String> registerPatterns) {
        this.registerPatterns = registerPatterns;
        this.compiledRegisterPatterns = null; // invalidate cache
    }

    public Map<String, List<String>> getDepartments() { return departments; }
    public void setDepartments(Map<String, List<String>> departments) {
        this.departments = departments;
        this.flatAliasMap = null; // invalidate cache
    }

    public List<String> getNoisePatterns() { return noisePatterns; }
    public void setNoisePatterns(List<String> noisePatterns) {
        this.noisePatterns = noisePatterns;
        this.compiledNoisePatterns = null; // invalidate cache
    }

    public int getMaxTokensPerLine() { return maxTokensPerLine; }
    public void setMaxTokensPerLine(int maxTokensPerLine) { this.maxTokensPerLine = maxTokensPerLine; }

    public int getSlidingWindowMax() { return slidingWindowMax; }
    public void setSlidingWindowMax(int slidingWindowMax) { this.slidingWindowMax = slidingWindowMax; }

    public String getTempDir() { return tempDir; }
    public void setTempDir(String tempDir) { this.tempDir = tempDir; }

    /**
     * Returns compiled register regex patterns (cached).
     */
    public List<Pattern> getCompiledRegisterPatterns() {
        if (compiledRegisterPatterns == null && registerPatterns != null) {
            compiledRegisterPatterns = registerPatterns.stream()
                    .map(Pattern::compile)
                    .toList();
        }
        return compiledRegisterPatterns;
    }

    /**
     * Returns flattened alias map: alias (UPPERCASED) → canonical department code.
     * Pre-normalized at load time. All lookups operate in uppercase space.
     */
    public Map<String, String> getFlatAliasMap() {
        if (flatAliasMap == null && departments != null) {
            flatAliasMap = new HashMap<>();
            for (var entry : departments.entrySet()) {
                String canonical = entry.getKey().toUpperCase();
                for (String alias : entry.getValue()) {
                    flatAliasMap.put(alias.toUpperCase(), canonical);
                }
            }
        }
        return flatAliasMap;
    }

    /**
     * Returns compiled noise patterns (cached, case-insensitive).
     */
    public List<Pattern> getCompiledNoisePatterns() {
        if (compiledNoisePatterns == null && noisePatterns != null) {
            compiledNoisePatterns = noisePatterns.stream()
                    .map(p -> Pattern.compile(p, Pattern.CASE_INSENSITIVE))
                    .toList();
        }
        return compiledNoisePatterns;
    }

    /**
     * Fail-fast validation at startup. If any config is invalid, the application
     * refuses to start rather than silently breaking at parse time.
     */
    @PostConstruct
    public void validate() {
        logger.info("Validating parser configuration...");

        // Register patterns must exist and compile
        if (registerPatterns == null || registerPatterns.isEmpty()) {
            throw new IllegalStateException("parser.register-patterns must not be empty");
        }
        for (String pattern : registerPatterns) {
            try {
                Pattern.compile(pattern);
            } catch (PatternSyntaxException e) {
                throw new IllegalStateException(
                        "Invalid register pattern '" + pattern + "': " + e.getMessage());
            }
        }

        // Department alias map must exist and not be empty
        if (departments == null || departments.isEmpty()) {
            throw new IllegalStateException("parser.departments must not be empty");
        }
        for (var entry : departments.entrySet()) {
            if (entry.getValue() == null || entry.getValue().isEmpty()) {
                throw new IllegalStateException(
                        "Department '" + entry.getKey() + "' has empty alias list");
            }
        }

        // Limits must be positive
        if (maxFileSizeMb <= 0) {
            throw new IllegalStateException("parser.max-file-size-mb must be > 0");
        }
        if (maxTokensPerLine <= 0) {
            throw new IllegalStateException("parser.max-tokens-per-line must be > 0");
        }
        if (slidingWindowMax <= 0) {
            throw new IllegalStateException("parser.sliding-window-max must be > 0");
        }

        // Pre-build caches to catch issues at startup
        getCompiledRegisterPatterns();
        getFlatAliasMap();
        if (noisePatterns != null) {
            for (String pattern : noisePatterns) {
                try {
                    Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
                } catch (PatternSyntaxException e) {
                    throw new IllegalStateException(
                            "Invalid noise pattern '" + pattern + "': " + e.getMessage());
                }
            }
            getCompiledNoisePatterns();
        }

        logger.info("Parser config validated: {} register patterns, {} departments, {} noise patterns",
                registerPatterns.size(), departments.size(),
                noisePatterns != null ? noisePatterns.size() : 0);

        // Compute config version hash for cache invalidation
        this.configVersion = computeConfigVersion();
        logger.info("Parser config version: {}", configVersion);
    }

    /**
     * Returns a deterministic hash of all config that affects parse output.
     * Changes to regex patterns, department aliases, or noise patterns
     * will produce a new version, invalidating cached parse results.
     */
    public String getConfigVersion() {
        return configVersion;
    }

    private String computeConfigVersion() {
        StringBuilder sb = new StringBuilder();
        if (registerPatterns != null) registerPatterns.forEach(sb::append);
        if (departments != null) {
            departments.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(e -> {
                        sb.append(e.getKey());
                        e.getValue().forEach(sb::append);
                    });
        }
        if (noisePatterns != null) noisePatterns.forEach(sb::append);
        sb.append(maxTokensPerLine).append(slidingWindowMax);

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(sb.toString().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash).substring(0, 16); // short hash
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
