package com.central.security.core.urls.model;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.Getter;

/**
 * Matches HTTP request paths against endpoint patterns.
 * Supports:
 * - Exact paths: /api/users
 * - Path variables: /api/users/{id} matches /api/users/123
 * - Path variables with regex: /{path:^(?!api)[^.]*}/**
 * - Wildcards: /api/admin/** matches any deeper paths
 * <p>
 * Follows Spring's PathPattern semantics for consistency with Spring Security.
 * <p>
 * <b>Design rule:</b> API endpoints (starting with /api) must not contain more
 * than one path variable. Use @RequestParam or @RequestBody instead.
 */
@Getter
public class EndpointPatternMatcher {
    /** Max number of path variables allowed in a single API endpoint. */
    public static final int MAX_PATH_VARIABLES = 1;

    private static final Pattern PATH_VAR_PATTERN = Pattern.compile("\\{([^}]+)\\}");

    private final String pattern;
    private final Pattern compiledPattern;
    private final boolean hasPathVariables;
    private final int pathVariableCount;

    public EndpointPatternMatcher(String pattern) {
        this.pattern = pattern;
        this.pathVariableCount = countPathVariables(pattern);
        this.hasPathVariables = pathVariableCount > 0;
        this.compiledPattern = compilePattern(pattern);
    }

    /**
     * Validate that the endpoint pattern respects API design rules.
     *
     * @throws IllegalArgumentException if the pattern violates rules
     */
    public void validateApiDesignRules() {
        if (pattern.startsWith("/api") && pathVariableCount > MAX_PATH_VARIABLES) {
            throw new IllegalArgumentException(
                    String.format(
                            "API endpoint '%s' contains %d path variables, but the maximum allowed is %d. "
                                    + "Use @RequestParam or @RequestBody for additional parameters.",
                            pattern, pathVariableCount, MAX_PATH_VARIABLES));
        }
    }

    /**
     * Check if the given request path matches this pattern.
     *
     * @param requestPath The actual HTTP request path (e.g., /api/users/123)
     * @return true if path matches this pattern
     */
    public boolean matches(String requestPath) {
        return compiledPattern.matcher(requestPath).matches();
    }

    private static int countPathVariables(String pattern) {
        Matcher m = PATH_VAR_PATTERN.matcher(pattern);
        int count = 0;
        while (m.find()) {
            // {name:regex} is a Spring regex constraint, not a true path variable
            if (!m.group(1).contains(":")) {
                count++;
            }
        }
        return count;
    }

    /**
     * Convert the endpoint pattern to regex.
     * <p>
     * Handles three kinds of brace expressions:
     * <ul>
     *   <li>{name} → [^/]+ (matches one path segment)</li>
     *   <li>{name:regex} → (regex) (uses the embedded regex)</li>
     * </ul>
     * Literal segments are regex-escaped. ** and * wildcards are supported.
     *
     * Examples:
     *   /api/users                          → ^/api/users$
     *   /api/users/{id}                     → ^/api/users/[^/]+$
     *   /api/admin/**                       → ^/api/admin/.*$
     *   /{path:^(?!api)[^.]*}/**            → ^/(?:(?!api)[^.]*)(?:/.*)?$
     */
    private static Pattern compilePattern(String pattern) {
        Matcher m = PATH_VAR_PATTERN.matcher(pattern);
        List<String> parts = new ArrayList<>();
        int lastEnd = 0;

        while (m.find()) {
            // Add literal text before this {…}
            if (m.start() > lastEnd) {
                parts.add(escapeLiteral(pattern.substring(lastEnd, m.start())));
            }
            String content = m.group(1);
            int colonIdx = content.indexOf(':');
            if (colonIdx >= 0) {
                // {name:regex} – use the embedded regex wrapped in a non-capturing group
                String regex = content.substring(colonIdx + 1);
                parts.add("(?:" + regex + ")");
            } else {
                // {name} – match one path segment
                parts.add("[^/]+");
            }
            lastEnd = m.end();
        }

        // Remaining literal tail
        if (lastEnd < pattern.length()) {
            parts.add(escapeLiteral(pattern.substring(lastEnd)));
        }

        String joined = String.join("", parts);
        return Pattern.compile("^" + joined + "$");
    }

    /**
     * Escape a literal segment, then convert * / ** wildcards.
     */
    private static String escapeLiteral(String segment) {
        // Escape all regex-special chars except * (handled below)
        String escaped = segment.replaceAll("([.+?^$|\\\\()\\[\\]])", "\\\\$1");

        // ** → match any number of path segments (including separators)
        escaped = escaped.replaceAll("\\*\\*", "(?:/.*)?");

        // Single * → match within one segment (no /)
        escaped = escaped.replaceAll("(?<!\\.)\\*(?!\\*)", "[^/]*");

        return escaped;
    }
}
