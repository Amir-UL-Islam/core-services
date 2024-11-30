package com.central.security.model.entites;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "users")
public class Users {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Date created;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(unique = true, nullable = false)
    private String username;

    private String email;

    private String phone;

    private Byte gender;

    @Column(length = 512, nullable = false)
    private String password;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "users_roles",
            joinColumns = @JoinColumn(name = "users_id", referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(name = "roles_id", referencedColumnName = "id")
    )
    private List<Role> roles = new ArrayList<>();

    private boolean enabled = true;

    private boolean accountNonExpired = true;

    private boolean accountNonLocked = true;

    private boolean credentialsNonExpired = true;

    private String agreementId;

    private String payerReference;

    private String customerMsisdn;

    private Boolean selfRegistered = false;

    public Users() {
    }

    public Users(UserAuth auth) {
        if (auth == null) throw new IllegalArgumentException("User can not be null!");
        this.setId(auth.getId());
        this.firstName = auth.getFirstName();
        this.lastName = auth.getLastName();
        this.username = auth.getUsername();
        this.password = auth.getPassword();
        this.phone = auth.getPhone();
        this.email = auth.getEmail();
        this.enabled = auth.isEnabled();
        this.roles = auth.getRoles();
        this.accountNonExpired = auth.isAccountNonExpired();
        this.accountNonLocked = auth.isAccountNonLocked();
        this.credentialsNonExpired = auth.isCredentialsNonExpired();
    }

    public void grantRole(Role role) {
        if (this.roles == null)
            this.roles = new ArrayList<>();
        // check if user already has that role
        if (!hasRole(role) && !role.isAdmin())
            this.roles.add(role);
    }

    public boolean canLogin() {
        return this.enabled
                && this.accountNonExpired
                && this.accountNonLocked
                && this.credentialsNonExpired;
    }

    public boolean hasRole(Role role) {
        return this.roles != null && this.roles.stream().anyMatch(r -> r.isSameAs(role));
    }

    public boolean isAdmin() {
        return this.roles != null &&
                this.roles.stream().anyMatch(Role::isAdmin);
    }

    public List<Long> getRoleIdList() {
        List<Long> roleIds = new ArrayList<>();
        if (this.roles != null) {
            for (Role role : this.roles) {
                roleIds.add(role.getId());
            }
        }
        return roleIds;
    }

    public boolean isSameUsername(String username) {
        if (username == null) return false;
        return username.trim().equals(this.username) || ("+88" + username.trim()).equals(this.username);
    }

    public String getName() {
        if (lastName == null) return firstName;
        return firstName + " " + lastName;
    }
}
