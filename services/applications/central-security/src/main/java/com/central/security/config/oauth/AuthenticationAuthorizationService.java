//package com.central.security.config;
//
//import com.central.security.model.dtos.request.Authenticate;
//import com.central.security.model.dtos.request.Registration;
//import com.central.security.model.dtos.response.SuccessfulAuthentication;
//import com.central.security.model.entites.Users;
//import com.central.security.model.enums.Role;
//import com.central.security.model.mappers.UserMappers;
//import com.central.security.repositories.UsersRepository;
//import com.central.security.services.RoleService;
//import jakarta.transaction.Transactional;
//import lombok.RequiredArgsConstructor;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.http.HttpStatus;
//import org.springframework.security.authentication.AuthenticationManager;
//import org.springframework.security.authentication.LockedException;
//import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
//import org.springframework.security.core.Authentication;
//import org.springframework.security.core.authority.SimpleGrantedAuthority;
//import org.springframework.security.core.userdetails.UsernameNotFoundException;
//import org.springframework.security.oauth2.core.AuthorizationGrantType;
//import org.springframework.security.oauth2.core.OAuth2AccessToken;
//import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
//import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
//import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
//import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
//import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
//import org.springframework.security.oauth2.server.authorization.token.DefaultOAuth2TokenContext;
//import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenContext;
//import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;
//import org.springframework.stereotype.Service;
//import org.springframework.web.server.ResponseStatusException;
//
//import java.security.Principal;
//import java.util.stream.Collectors;
//
//@Service
//@RequiredArgsConstructor
//public class AuthenticationAuthorizationService {
//    private final AuthenticationManager authenticationManager;
//    private final UsersRepository repository;
//    private final RegisteredClientRepository registeredClientRepository;
//    private final OAuth2AuthorizationService authorizationService;
//    private final OAuth2TokenGenerator<?> tokenGenerator;
//    private final UserMappers mappers;
//    private final RoleService roleService;
//
//    private static final Logger logger = LoggerFactory.getLogger(AuthenticationAuthorizationService.class);
//
//    @Transactional
//    public SuccessfulAuthentication register(Registration dto) {
//        Users user = mappers.map(dto);
//        user.getRoles().add(roleService.findByName(Role.USER.name()));
//        repository.save(user);
//
//        // Issue OAuth2 token
//        String token = issueToken(user);
//        return SuccessfulAuthentication.builder()
//                .username(user.getUsername())
//                .token(token)
//                .build();
//    }
//
//    public SuccessfulAuthentication login(Authenticate dto) {
//        try {
//            authenticationManager.authenticate(
//                    new UsernamePasswordAuthenticationToken(
//                            dto.getUsername(),
//                            dto.getPassword()
//                    )
//            );
//        } catch (LockedException e) {
//            throw new ResponseStatusException(
//                    HttpStatus.UNAUTHORIZED, "User account is locked", e);
//        }
//        Users user = repository.findByUsername(dto.getUsername()).orElseThrow(
//                () -> new UsernameNotFoundException("User not found")
//        );
//
//        // Issue OAuth2 token
//        String token = issueToken(user);
//        return SuccessfulAuthentication.builder()
//                .username(user.getUsername())
//                .token(token)
//                .build();
//    }
//
//
//    private String issueToken(Users user) {
//        RegisteredClient registeredClient = registeredClientRepository.findByClientId("your-client-id");
//        if (registeredClient == null) {
//            throw new IllegalArgumentException("Registered client not found");
//        }
//
//        OAuth2Authorization authorization = OAuth2Authorization.withRegisteredClient(registeredClient)
//                .principalName(user.getUsername())
//                .authorizationGrantType(AuthorizationGrantType.PASSWORD)
//                .attribute(Principal.class.getName(), user)
//                .build();
//
//        OAuth2TokenContext tokenContext = DefaultOAuth2TokenContext.builder()
//                .registeredClient(registeredClient)
//                .principal(this.createAuthenticationFromUser(user))
//                .authorization(authorization)
//                .tokenType(OAuth2TokenType.ACCESS_TOKEN)
//                .authorizationGrantType(AuthorizationGrantType.PASSWORD)
//                .build();
//
//        logger.debug("Token context: {}", tokenContext);
//
//        OAuth2AccessToken accessToken = (OAuth2AccessToken) tokenGenerator.generate(tokenContext);
//        if (accessToken == null) {
//            throw new IllegalArgumentException("Token cannot be null");
//        }
//
//        authorization = OAuth2Authorization.from(authorization)
//                .token(accessToken)
//                .build();
//
//        authorizationService.save(authorization);
//
//        return accessToken.getTokenValue();
//    }
//
//    public Authentication createAuthenticationFromUser(Users user) {
//        return new UsernamePasswordAuthenticationToken(
//                user.getUsername(),
//                null, // No credentials required for an authenticated user
//                user.getRoles().stream()
//                        .map(role -> new SimpleGrantedAuthority(role.getName()))
//                        .collect(Collectors.toList())
//        );
//    }
//}