package com.central.security.core.security.oauth;

import com.central.security.core.role.model.enums.SystemRoles;
import com.central.security.core.users.model.entity.Users;
import com.central.security.core.users.service.UsersService;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class SecurityContext {
    private static UsersService userService;
    private static Users auditUser;

    public SecurityContext(UsersService userService) {
        SecurityContext.userService = userService;
    }

    public static Users getCurrentLoggedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && !(authentication instanceof AnonymousAuthenticationToken)) {
            if (auditUser == null || !authentication.getName().equals(auditUser.getUsername())) {
                auditUser = userService.findByUsernameIgnoreCase(authentication.getName());
            }
            return auditUser;
        }
        return null;
    }

    public static Boolean isSuperAdmin() {
        Users user = getCurrentLoggedUser();
        return user != null && user.getRole()
                .stream()
                .anyMatch(role -> role.getName().equals(SystemRoles.SUPER_ADMIN.name()));
    }
}
