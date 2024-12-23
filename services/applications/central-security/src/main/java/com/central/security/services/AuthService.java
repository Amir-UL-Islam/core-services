package com.central.security.services;

import com.central.security.model.dtos.request.Authenticate;
import com.central.security.model.dtos.request.Registration;
import com.central.security.model.dtos.response.SuccessfulAuthentication;
import com.central.security.model.entites.Users;
import com.central.security.model.enums.Roles;
import com.central.security.model.mappers.UserMappers;
import com.central.security.repositories.UsersRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;


@Service
@RequiredArgsConstructor
public class AuthService {
    private final AuthenticationManager authenticationManager;
    private final UsersRepository repository;
    private final JwtTokenService jwtTokenService;
    private final UserMappers mappers;
    private final RoleService roleService;

    @Transactional
    public SuccessfulAuthentication register(Registration dto) {
        Users user = mappers.map(dto);
        user.getRoles().add(roleService.findByName(Roles.USER));
        repository.save(user);

        String token = jwtTokenService.createToken(user);
        return SuccessfulAuthentication.builder()
                .username(user.getUsername())
                .token(token)
                .build();
    }

    public SuccessfulAuthentication login(Authenticate dto) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            dto.getUsername(),
                            dto.getPassword()
                    )
            );
        } catch (LockedException e) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "User account is locked", e);
        }
        Users user = repository.findByUsername(dto.getUsername()).orElseThrow(
                () -> new UsernameNotFoundException("User not found")
        );
        String token = jwtTokenService.createToken(user);
        return SuccessfulAuthentication.builder()
                .username(user.getUsername())
                .token(token)
                .build();
    }
}
