package com.central.security.core.security.filters;


import com.central.security.core.privilege.service.implmentation.PrivilegeService;
import com.central.security.core.security.PublicApiPaths;
import com.central.security.core.security.audit.service.AuthorizationDecisionAuditService;
import com.central.security.core.users.model.entity.Users;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class ACLFilter extends OncePerRequestFilter {

    private final PrivilegeService privilegeService;
    private final AuthorizationDecisionAuditService auditService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        final String uri = request.getRequestURI();

        // Policy-first ACL applies only to /api routes; static assets pass through.
        if (!uri.startsWith("/api")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Public API paths are exempt from authentication and authorization.
        if (PublicApiPaths.isPublic(request.getMethod(), uri)) {
            filterChain.doFilter(request, response);
            return;
        }

        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // Reject unauthenticated (null, not-authenticated, or anonymous) requests.
        if (!isFullyAuthenticated(authentication)) {
            log.warn("Unauthenticated access attempt: {} {}", request.getMethod(), uri);
            auditService.recordDenied(null, "anonymous", uri, request.getMethod(),
                    authentication, "Not authenticated");
            writeJsonError(response, HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized", "Authentication required");
            return;
        }

        // Check authorization and record decision.
        if (!privilegeService.hasPermission(authentication, request)) {
            String username = null;
            Long userId = null;

            if (authentication.getPrincipal() instanceof UserDetails userDetails) {
                username = userDetails.getUsername();
                if (userDetails instanceof Users) {
                    userId = ((Users) userDetails).getId();
                }
            }

            final String reason = "User does not have required privilege for this endpoint";
            log.warn("Access denied for user {} attempting {} {}", username, request.getMethod(), uri);
            auditService.recordDenied(userId, username, uri, request.getMethod(),
                    authentication, reason);

            writeJsonError(response, HttpServletResponse.SC_FORBIDDEN, "Forbidden", reason);
            return;
        }

        // Access allowed — record audit trail for sensitive (modifying) operations.
        if (isModifyingOperation(request.getMethod())) {
            String username = null;
            Long userId = null;

            if (authentication.getPrincipal() instanceof UserDetails userDetails) {
                username = userDetails.getUsername();
                if (userDetails instanceof Users) {
                    userId = ((Users) userDetails).getId();
                }
            }

            auditService.recordAllowed(userId, username, uri, request.getMethod(), authentication);
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Returns {@code true} only for fully-authenticated (non-anonymous) users.
     * <p>
     * {@link AnonymousAuthenticationToken#isAuthenticated()} returns {@code true} in
     * Spring Security, so we must explicitly reject it here — anonymous users that
     * reach a protected path should receive 401 (not 403).
     */
    private boolean isFullyAuthenticated(final Authentication auth) {
        return auth != null
                && auth.isAuthenticated()
                && !(auth instanceof AnonymousAuthenticationToken);
    }

    private boolean isModifyingOperation(final String method) {
        return method != null && (method.equals("POST") || method.equals("PUT") ||
                                  method.equals("DELETE") || method.equals("PATCH"));
    }

    private void writeJsonError(HttpServletResponse response, int status, String error, String message)
            throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(
                String.format("{\"status\":%d,\"error\":\"%s\",\"message\":\"%s\"}", status, error, message));
    }
}
