package com.central.security.controllers;

import com.central.security.model.dtos.PrivilegeDTO;
import com.central.security.services.PrivilegeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class PrivilegeController {
    private final PrivilegeService privilegeService;

    @PostMapping("/api/v1/privileges")
    public void savePrivilege(@RequestBody PrivilegeDTO dto) {
        privilegeService.save(dto);
    }

}
