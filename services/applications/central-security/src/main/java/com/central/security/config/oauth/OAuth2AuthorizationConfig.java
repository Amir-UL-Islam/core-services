//package com.central.security.config;
//
//import com.nimbusds.jose.jwk.JWKSet;
//import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
//import com.nimbusds.jose.jwk.source.JWKSource;
//import com.nimbusds.jose.proc.SecurityContext;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.security.oauth2.core.AuthorizationGrantType;
//import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
//import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
//import org.springframework.security.oauth2.server.authorization.InMemoryOAuth2AuthorizationService;
//import org.springframework.security.oauth2.server.authorization.client.InMemoryRegisteredClientRepository;
//import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
//import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
//import org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat;
//import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
//import org.springframework.security.oauth2.server.authorization.token.DelegatingOAuth2TokenGenerator;
//import org.springframework.security.oauth2.server.authorization.token.OAuth2AccessTokenGenerator;
//import org.springframework.security.oauth2.server.authorization.token.OAuth2RefreshTokenGenerator;
//import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;
//
//import java.time.Duration;
//
//@Configuration
//public class OAuth2AuthorizationConfig {
//
//    @Bean
//    public RegisteredClientRepository registeredClientRepository() {
//        RegisteredClient registeredClient = RegisteredClient.withId("your-client-id")
//                .clientId("your-client-id")
//                .clientSecret("{noop}your-client-secret")
//                .authorizationGrantType(AuthorizationGrantType.PASSWORD)
//                .scope("read")
//                .scope("write")
//                .tokenSettings(TokenSettings.builder()
//                        .accessTokenFormat(OAuth2TokenFormat.SELF_CONTAINED)
//                        .accessTokenTimeToLive(Duration.ofHours(1))
//                        .build())
//                .build();
//
//        return new InMemoryRegisteredClientRepository(registeredClient);
//    }
//    @Bean
//    public OAuth2AuthorizationService authorizationService() {
//        return new InMemoryOAuth2AuthorizationService();
//    }
//
//    @Bean
//    public OAuth2TokenGenerator<?> tokenGenerator() {
//        return new DelegatingOAuth2TokenGenerator(
//                new OAuth2AccessTokenGenerator(),
//                new OAuth2RefreshTokenGenerator()
//        );
//    }
//
//}