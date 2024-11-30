package com.central.security.model.entites;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;

import java.util.Date;
import java.util.List;

@Setter
@Getter
@Entity
public class Privilege {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Date created;
    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false, unique = true)
    private String label;

    private String description;

    @ElementCollection
    @LazyCollection(LazyCollectionOption.FALSE)
    @CollectionTable(name = "privileges_access_urls")
    private List<String> accessUrls;

    public Privilege() {
        // Default constructor
    }

    public Privilege(String name, String label) {
        this.name = name;
        this.label = label;
    }

    public String[] accessesArr() {
        return accessUrls.toArray(new String[0]);
    }

    public String accessesStr() {
        return String.join(", ", accessUrls);
    }


    @Getter
    public enum Privileges {
        ADMINISTRATION("Administration"),
        ACCESS_USER_RESOURCES("Access User Resources");

        private final String label;

        Privileges(String label) {
            this.label = label;
        }

    }
}
