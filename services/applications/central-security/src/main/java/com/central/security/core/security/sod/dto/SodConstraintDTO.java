package com.central.security.core.security.sod.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SodConstraintDTO {

    private Long id;

    @NotBlank
    @Size(max = 100)
    private String roleNameA;

    @NotBlank
    @Size(max = 100)
    private String roleNameB;

    @Size(max = 500)
    private String reason;

    /**
     * false = static SoD: user can NEVER hold both roles simultaneously (co-assignment forbidden).
     * true  = dynamic SoD: user may hold both roles but cannot activate both in the same session.
     */
    private boolean dynamic = false;
}
