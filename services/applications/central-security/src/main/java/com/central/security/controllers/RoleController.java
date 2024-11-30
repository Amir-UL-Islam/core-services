package com.central.security.controllers;

import com.central.security.model.dtos.RoleDTO;
import com.central.security.services.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class RoleController {
    private final RoleService roleService;

    @PostMapping("/api/v1/roles")
    public void saveRole(@RequestBody RoleDTO dto) {
        roleService.save(dto);
    }
}
