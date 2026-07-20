package com.central.security.core.role.model.entity;

import com.central.security.core.privilege.model.entity.Privilege;
import com.central.security.core.users.model.entity.Users;
import com.problemfighter.java.base.entity.BaseUserEntity;
import com.problemfighter.pfspring.restapi.inter.model.RestEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;

import java.util.HashSet;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;


@Entity
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public class Role extends BaseUserEntity implements RestEntity {

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false, unique = true)
    private String description;

    @ManyToMany(mappedBy = "role")
    private Set<Users> user = new HashSet<>();

    /**
     * Parent role in the RBAC hierarchy. When set, this role inherits all privileges
     * of the parent (and transitively all ancestors). Null means this is a root role.
     * Stored as a self-referential FK (parent_role_id).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_role_id")
    private Role parent;

    @ManyToMany
    @JoinTable(
            name = "RolePrivilege",
            joinColumns = @JoinColumn(name = "roleId"),
            inverseJoinColumns = @JoinColumn(name = "privilegeId")
    )
    private Set<Privilege> privilege = new HashSet<>();



}
