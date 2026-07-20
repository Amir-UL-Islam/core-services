package com.central.security.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.central.security.core.role.service.RoleHierarchyService;
import com.central.security.core.security.PublicApiPaths;
import com.central.security.core.security.jwt.CustomUserDetailsService;
import com.central.security.core.security.filters.ACLFilter;
import com.central.security.core.security.filters.RequestBodyCachingFilter;
import com.central.security.core.security.interceptor.CustomAccessDeniedHandler;
import com.central.security.core.security.interceptor.CustomAuthenticationEntryPoint;

import java.security.interfaces.RSAPublicKey;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;


@Configuration
@EnableMethodSecurity(prePostEnabled = true)
public class JwtSecurityConfig {

    private final ACLFilter aclPermissionFilter;
    private final RequestBodyCachingFilter requestBodyCachingFilter;
    private final RoleHierarchyService roleHierarchyService;

    public JwtSecurityConfig(final ACLFilter aclPermissionFilter,
                             final RequestBodyCachingFilter requestBodyCachingFilter,
                             final RoleHierarchyService roleHierarchyService) {
        this.aclPermissionFilter = aclPermissionFilter;
        this.requestBodyCachingFilter = requestBodyCachingFilter;
        this.roleHierarchyService = roleHierarchyService;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    /**
     * Data-driven RBAC role hierarchy loaded from DB via {@link RoleHierarchyService}.
     * The result is Caffeine-cached and refreshed on hierarchy changes.
     */
    @Bean
    public RoleHierarchy roleHierarchy() {
        return authorities -> roleHierarchyService.buildSpringHierarchy()
                .getReachableGrantedAuthorities(authorities);
    }

    @Bean
    public AuthenticationProvider authenticationProvider(final PasswordEncoder passwordEncoder,
                                                         final CustomUserDetailsService jwtUserDetailsService) {
        final DaoAuthenticationProvider authenticationProvider = new DaoAuthenticationProvider(jwtUserDetailsService);
        authenticationProvider.setPasswordEncoder(passwordEncoder);
        return authenticationProvider;
    }

    @Bean
    public JwtDecoder jwtDecoder(@Value("classpath:certs/public.pem") final RSAPublicKey publicKey) {
        return NimbusJwtDecoder.withPublicKey(publicKey).build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        final JwtGrantedAuthoritiesConverter delegate = new JwtGrantedAuthoritiesConverter();
        delegate.setAuthorityPrefix(""); // keep roles as-is
        delegate.setAuthoritiesClaimName("authorities");

        final JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(delegate);
        return converter;
    }

    /**
     * Custom {@link BearerTokenResolver} that skips JWT extraction for public paths.
     * <p>
     * Without this, Spring Security's {@link BearerTokenAuthenticationFilter} attempts to
     * validate any {@code Authorization: Bearer ...} header on a {@code permitAll()} endpoint
     * and — if the token is expired or malformed — invokes {@link CustomAuthenticationEntryPoint}
     * with a 401 <em>before</em> the {@code permitAll()} rule or {@code ACLFilter} can act.
     * <p>
     * Returning {@code null} for public paths causes the filter chain to continue with
     * anonymous authentication instead of attempting token validation.
     */
    @Bean
    public BearerTokenResolver bearerTokenResolver() {
        final DefaultBearerTokenResolver delegate = new DefaultBearerTokenResolver();
        return request -> {
            if (PublicApiPaths.isPublic(request.getMethod(), request.getRequestURI())) {
                return null;
            }
            return delegate.resolve(request);
        };
    }

    @Bean
    public SecurityFilterChain jwtFilterChain(final HttpSecurity http,
                                              final JwtAuthenticationConverter jwtAuthenticationConverter,
                                              final BearerTokenResolver bearerTokenResolver,
                                              final CustomAuthenticationEntryPoint customAuthenticationEntryPoint,
                                              final CustomAccessDeniedHandler customAccessDeniedHandler) throws Exception {
        return http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin))
                .authorizeHttpRequests(authorize -> authorize
                        // POST-only public paths (authentication, registration, addresses)
                        .requestMatchers(HttpMethod.POST, PublicApiPaths.postPaths())
                        .permitAll()
                        // GET-only public paths (geo-node cascading dropdowns)
                        .requestMatchers(HttpMethod.GET, PublicApiPaths.getPaths())
                        .permitAll()
                        // All-method public paths (/api/v1/public/**, static assets, Swagger)
                        .requestMatchers(PublicApiPaths.allMethodPaths())
                        .permitAll()
                        .anyRequest()
                        .authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .bearerTokenResolver(bearerTokenResolver)
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(requestBodyCachingFilter, BearerTokenAuthenticationFilter.class)
                .addFilterAfter(aclPermissionFilter, BearerTokenAuthenticationFilter.class)
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(customAuthenticationEntryPoint)
                        .accessDeniedHandler(customAccessDeniedHandler))
                .build();
    }

    @Bean
    public CustomAccessDeniedHandler customAccessDeniedHandler(final ObjectMapper objectMapper) {
        return new CustomAccessDeniedHandler(objectMapper);
    }

    @Bean
    public CustomAuthenticationEntryPoint customAuthenticationEntryPoint(final ObjectMapper objectMapper) {
        return new CustomAuthenticationEntryPoint(objectMapper);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        final CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(
                "https://nicu-dev.cmedhealth.com",
                "https://nicu-management-system.vercel.app",
                "http://localhost:9988",
                "http://localhost:3000")
        );
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setExposedHeaders(List.of("Authorization"));
        config.setMaxAge(3600L);

        final UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
