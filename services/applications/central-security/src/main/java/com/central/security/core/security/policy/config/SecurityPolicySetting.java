package com.central.security.core.security.policy.config;

import com.problemfighter.java.base.entity.BaseUserEntity;
import com.problemfighter.pfspring.restapi.inter.model.RestEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@EqualsAndHashCode(callSuper = true)
@Data
@Entity
@EntityListeners(AuditingEntityListener.class)
public class SecurityPolicySetting extends BaseUserEntity implements RestEntity {

    @Column(nullable = false, unique = true)
    private String policyKey;

    @Column(nullable = false, length = 4000)
    private String policyValue;
}

