package com.central.security.model.entites;

import com.central.security.model.enums.Permissions;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Setter
@Getter
@Entity
public class Privilege {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Date created;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false, unique = true)
    private String label;

    private String description;

    @Column(name = "url_pattern", unique = true, nullable = false)
    private String urlPattern;

    @Column(name = "http_method", nullable = false)
    private String httpMethod;

    @Column(name = "permission_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private Permissions permissions;
}
