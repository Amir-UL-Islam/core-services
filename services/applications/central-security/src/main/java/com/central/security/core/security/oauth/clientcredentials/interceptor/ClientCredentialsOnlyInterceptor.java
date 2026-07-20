package com.central.security.core.security.oauth.clientcredentials.interceptor;

import com.central.security.core.security.oauth.clientcredentials.annotation.MakeThisAvailableForMachineClient;
import com.central.security.core.security.oauth.clientcredentials.service.ClientCredentialsService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@Slf4j
public class ClientCredentialsOnlyInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(final HttpServletRequest request,
                             final HttpServletResponse response,
                             final Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        final Set<String> requiredScopes = resolveRequiredScopes(handlerMethod);
        if (requiredScopes == null) {
            return true;
        }

        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return true;
        }

        final Jwt jwt = extractJwt(authentication.getPrincipal());
        // This annotation is additive: keep normal user access untouched.
        if (jwt == null || !"client_credentials".equals(jwt.getClaimAsString("grant_type"))) {
            return true;
        }

        final Set<String> authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        if (!authorities.contains(ClientCredentialsService.CLIENT_CREDENTIALS_AUTHORITY)) {
            writeForbidden(response, "Client credentials authority missing");
            return false;
        }

        for (final String scope : requiredScopes) {
            final String required = "SCOPE_" + scope;
            if (!authorities.contains(required)) {
                writeForbidden(response, "Missing required scope: " + scope);
                return false;
            }
        }

        return true;
    }

    private Set<String> resolveRequiredScopes(final HandlerMethod method) {
        final MakeThisAvailableForMachineClient methodAnnotation =
                AnnotatedElementUtils.findMergedAnnotation(method.getMethod(), MakeThisAvailableForMachineClient.class);
        if (methodAnnotation != null) {
            return new LinkedHashSet<>(Arrays.asList(methodAnnotation.scopes()));
        }

        final MakeThisAvailableForMachineClient classAnnotation =
                AnnotatedElementUtils.findMergedAnnotation(method.getBeanType(), MakeThisAvailableForMachineClient.class);
        if (classAnnotation != null) {
            return new LinkedHashSet<>(Arrays.asList(classAnnotation.scopes()));
        }

        return null;
    }

    private Jwt extractJwt(final Object principal) {
        if (principal instanceof Jwt jwt) {
            return jwt;
        }
        return null;
    }

    private void writeForbidden(final HttpServletResponse response, final String message) throws IOException {
        log.debug("Client credentials guard blocked request: {}", message);
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"status\":403,\"error\":\"Forbidden\",\"message\":\"" + message + "\"}");
    }
}

