package com.central.security.model.enums;

import lombok.Getter;

@Getter
public enum Permissions {
    ADMINISTRATION("Administration"),
    ACCESS_USER_RESOURCES("Access User Resources");

    private final String label;

    Permissions(String label) {
        this.label = label;
    }
}
