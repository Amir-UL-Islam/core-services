//package com.central.security.config;
//
//import com.central.security.model.entites.Role;
//import com.central.security.model.entites.UserAuth;
//import org.springframework.security.core.Authentication;
//import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
//import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
//import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
//import org.springframework.stereotype.Component;
//
//@Component
//public class CustomJwtCustomizer implements OAuth2TokenCustomizer<JwtEncodingContext> {
//
//    @Override
//    public void customize(JwtEncodingContext context) {
//        if (context.getTokenType().equals(OAuth2TokenType.ACCESS_TOKEN)) {
//            Authentication principal = context.getPrincipal();
//            if (principal.getPrincipal() instanceof UserAuth user) {
//                context.getClaims().claim("id", user.getId())
//                        .claim("first_name", user.getFirstName())
//                        .claim("last_name", user.getLastName())
//                        .claim("username", user.getUsername())
//                        .claim("authorities", user.getAuthorities())
//                        .claim("roles", user.getRoles().stream().map(Role::getName).toList().toString());
//            }
//        }
//    }
//}
