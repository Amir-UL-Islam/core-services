package com.central.security.model.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class PrivilegeDTO {
    private Long created;
    private String name;
    private String label;

    private String description;
    private List<String> accessUrls;

    public PrivilegeDTO(List<String> accessUrls, Long created, String description, String label, String name) {
        this.accessUrls = accessUrls;
        this.created = created;
        this.description = description;
        this.label = label;
        this.name = name;
    }
}
