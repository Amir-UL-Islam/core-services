package com.central.security.model.entites;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import java.util.Date;
import java.util.List;


@Setter
@Getter
@Entity
@Table(name = "roles")
public class Role {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Date created;

    @Column(nullable = false, unique = true)
    private String name;

    private String description;

    @ManyToMany(fetch = FetchType.EAGER)
    @Fetch(FetchMode.SUBSELECT)
    @JoinTable(
            name = "roles_privileges",
            joinColumns = @JoinColumn(name = "role_id", referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(name = "privilege_id", referencedColumnName = "id")
    )
    private List<Privilege> privileges;

    @Column(nullable = false)
    private boolean restricted = true;

    public Role() {
        // Default constructor
    }

    public boolean isAdmin() {
        return privileges != null && privileges.stream()
                .anyMatch(privilege -> Privilege.Privileges.ADMINISTRATION.name().equals(privilege.getName()));
    }

    public boolean isSameAs(Role role) {
        return role != null && this.getId().equals(role.getId());
    }

    public boolean hasPrivilege(Long privilegeId) {
        return privileges != null && privileges.stream()
                .anyMatch(privilege -> privilegeId.equals(privilege.getId()));
    }


}
