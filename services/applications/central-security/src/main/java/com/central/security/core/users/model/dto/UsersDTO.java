package com.central.security.core.users.model.dto;

import com.problemfighter.java.oc.annotation.DataMapping;
import com.problemfighter.java.oc.annotation.DataMappingInfo;
import com.problemfighter.pfspring.restapi.inter.model.RestDTO;
import com.problemfighter.java.base.dto.BaseUserDTO;
import com.central.security.core.security.annotation.GenderValue;
import com.central.security.core.security.validation.SecurityValidationPatterns;
import com.central.security.core.users.model.Relation;
import com.central.security.core.users.model.mapper.UsersInterceptor;
import com.central.security.core.users.UsersUsernameUnique;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

import lombok.Getter;
import lombok.Setter;


@DataMappingInfo(customProcessor = UsersInterceptor.class)
@Getter
@Setter
public class UsersDTO extends BaseUserDTO implements RestDTO {

    @Size(max = 255)
    private String name;

    @Size(max = 255)
    @Email(message = "Email must be a valid address")
    private String email;

    @Size(max = 32)
    @Pattern(regexp = SecurityValidationPatterns.BD_IN_CONTACT_REGEX,
            message = "Phone must be a valid Bangladeshi or Indian number")
    private String phone;

    @GenderValue
    private String gender;

    private Relation relation;

    @NotBlank
    @Size(max = 255)
    @UsersUsernameUnique
    private String username;

    /**
     * Plain-text password (CREATE flow only).
     * The {@code @DataMapping(source="_password")} annotation with a non-existent source name prevents the auto-mapper from
     * copying this field directly to Users. Password — which would overwrite the
     * BCrypt-encoded value that UsersInterceptor.meAsSrc already wrote.
     * The interceptor still reads it via source.getPassword().
     */
    @DataMapping(source = "_password")
    // @Size(min = 8, max = 72)
    private String password;

    /** New plain-text password (UPDATE flow). Handled solely by UsersInterceptor. */
    @DataMapping(source = "_newPassword")
    // @Size(min = 8, max = 72)
    private String newPassword;

    /** Current password confirmation (UPDATE flow). Never mapped to entity. */
    @DataMapping(source = "_currentPassword")
    // @Size(min = 8, max = 72)
    private String currentPassword;

    private List<Long> role;

    private Boolean twoFactorEnabled;

    private String totpSecret;

    private Boolean smsMfaEnabled;

    private Boolean emailMfaEnabled;

    private String preferredMfaFactor;

    private Boolean emailVerified;

    private Boolean phoneVerified;

    private Boolean accountEnabled;

}
