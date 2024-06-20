package com.central.security.model.mappers;

import com.central.security.model.dtos.request.Registration;
import com.central.security.model.entites.Users;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserMappers {
    private final PasswordEncoder passwordEncoder;

    public Users map(Registration dto) {
        Users user = new Users();

        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setEmail(dto.getEmail());
        user.setPhone(dto.getPhone());

        return user;
    }
}
