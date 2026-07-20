package com.central.security.core.security;

import org.springframework.util.AntPathMatcher;

/**
 * Single source of truth for all unauthenticated (public) API paths.
 * <p>
 * Both {@code JwtSecurityConfig} (Spring Security permit-all rules) and
 * {@code ACLFilter} (policy-first ACL bypass) reference this class, so that
 * adding a new public endpoint requires a change in exactly one place.
 *
 * <h3>Pattern syntax</h3>
 * Ant-style patterns are supported ({@code **} = any path segment, {@code *} = single segment).
 * {@code JwtSecurityConfig} passes these strings directly to {@code requestMatchers()};
 * {@code ACLFilter} uses {@link AntPathMatcher} for runtime matching.
 */
public final class PublicApiPaths {

    private PublicApiPaths() {}

    // ─── POST-only public paths ────────────────────────────────────────────────
    /**
     * POST endpoints that require no authentication.
     */
    public static final String[] POST_PATHS = {
            "/api/v1/authenticate",
            "/api/v1/authenticateGoogle",
            "/api/v1/oauth/token",
            "/api/v1/refresh-token",
            "/api/v1/register/init",
            "/api/v1/register/verify",
            "/api/v1/addresses",
            "/api/v1/file-storage/upload",
            "/api/v1/auth/forgot-password",
            "/api/v1/auth/reset-password"
    };

    // ─── GET-only public paths ─────────────────────────────────────────────────
    /**
     * GET endpoints that require no authentication (supports Ant wildcards).
     */
    public static final String[] GET_PATHS = {
            "/api/v1/geo-nodes",
            "/api/v1/geo-nodes/**",
    };

    // ─── All-method public paths ───────────────────────────────────────────────
    /**
     * Paths that are public for ALL HTTP methods (supports Ant wildcards).
     * <p>
     * The {@code /api/v1/public/**} prefix covers all hospital/ambulance self-registration
     * endpoints (and any future public API endpoints added under that prefix).
     */
    public static final String[] ALL_METHOD_PATHS = {
            "/api/v1/public/**",
            "/files/**",
            "/",
            "/index.html",
            "/static/**",
            "/assets/**",
            "/css/**",
            "/js/**",
            "/images/**",
            "/favicon.ico",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
    };

    // ─── Helpers ───────────────────────────────────────────────────────────────

    /** Returns the POST-only path array (for use in JwtSecurityConfig). */
    public static String[] postPaths() {
        return POST_PATHS;
    }

    /** Returns the GET-only path array (for use in JwtSecurityConfig). */
    public static String[] getPaths() {
        return GET_PATHS;
    }

    /** Returns the all-method path array (for use in JwtSecurityConfig). */
    public static String[] allMethodPaths() {
        return ALL_METHOD_PATHS;
    }

    // ─── Runtime matching (used by ACLFilter) ─────────────────────────────────

    private static final AntPathMatcher ANT = new AntPathMatcher();

    /**
     * Returns {@code true} if the given URI + HTTP method combination is a
     * public (unauthenticated) request, using Ant-style path matching.
     *
     * @param method HTTP method string (e.g. "GET", "POST")
     * @param uri    the request URI (no context path, no query string)
     */
    public static boolean isPublic(final String method, final String uri) {
        // All-method paths (static assets, Swagger, /api/v1/public/**)
        for (final String pattern : ALL_METHOD_PATHS) {
            if (ANT.match(pattern, uri)) {
                return true;
            }
        }
        // Method-specific paths
        if ("POST".equalsIgnoreCase(method)) {
            for (final String pattern : POST_PATHS) {
                if (ANT.match(pattern, uri)) {
                    return true;
                }
            }
        }
        if ("GET".equalsIgnoreCase(method)) {
            for (final String pattern : GET_PATHS) {
                if (ANT.match(pattern, uri)) {
                    return true;
                }
            }
        }
        return false;
    }
}
