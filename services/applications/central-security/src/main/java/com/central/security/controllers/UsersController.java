package com.central.security.controllers;

import com.central.security.model.entites.Users;
import com.central.security.services.UserServices;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class UsersController {
    private final UserServices service;


    @GetMapping("/welcome")
    public String root() {
        return "Welcome to the Central Security System!";
    }

    @GetMapping("/get-all")
    public List<Users> getUsers() {
        return service.getUsers();
    }

    @GetMapping("/test-admin-role")
    public String getTestAdminRole() {
        return "You have the admin role!";
    }

    @GetMapping("/test-user-role")
    public String getTestUserRole() {
        return "You have the user role!";
    }
}
