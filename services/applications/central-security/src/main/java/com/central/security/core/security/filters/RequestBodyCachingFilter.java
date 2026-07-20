package com.central.security.core.security.filters;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;

/**
 * Ensures request payload can be re-read later (e.g. custom validators) by wrapping
 * incoming requests in {@link ContentCachingRequestWrapper}.
 */
@Component
public class RequestBodyCachingFilter extends OncePerRequestFilter {

    public static final String CACHED_REQUEST_ATTRIBUTE = RequestBodyCachingFilter.class.getName() + ".WRAPPER";

    @Override
    protected void doFilterInternal(final HttpServletRequest request,
                                    final HttpServletResponse response,
                                    final FilterChain filterChain) throws ServletException, IOException {
        if (request instanceof ContentCachingRequestWrapper) {
            request.setAttribute(CACHED_REQUEST_ATTRIBUTE, request);
            filterChain.doFilter(request, response);
            return;
        }

        final ContentCachingRequestWrapper wrapped = new ContentCachingRequestWrapper(request, 1024 * 1024);
        wrapped.setAttribute(CACHED_REQUEST_ATTRIBUTE, wrapped);
        filterChain.doFilter(wrapped, response);
    }
}

