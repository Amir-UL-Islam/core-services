package com.central.security.controllers;

import com.central.security.model.dtos.request.Authenticate;
import com.central.security.model.dtos.request.Registration;
import com.central.security.model.dtos.response.SuccessfulAuthentication;
import com.central.security.services.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService service;
    @PostMapping("/register")
    public SuccessfulAuthentication register(@RequestBody Registration dto) {
        return service.register(dto);
    }

    @PostMapping("/login")
    public SuccessfulAuthentication login(@RequestBody Authenticate dto) {
        return service.login(dto);
    }
}
