package com.central.security.core.urls.model.entity;

import com.problemfighter.java.base.entity.BaseUserEntity;
import com.central.security.core.privilege.model.entity.Privilege;
import com.problemfighter.pfspring.restapi.inter.model.RestEntity;
import jakarta.persistence.*;

import java.util.HashSet;
import java.util.Set;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;


@Entity
@Table(
    uniqueConstraints = @UniqueConstraint(
        name = "uk_endpoint_method",
        columnNames = {"endpoint", "method"}
    )
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public class Url extends BaseUserEntity implements RestEntity {

    @Column(nullable = false)
    private String endpoint;

    @Column(nullable = false)
    private String method;

    @ManyToMany(mappedBy = "urls")
    private Set<Privilege> privileges = new HashSet<>();

}
