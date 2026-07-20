package com.central.security.core.security.validation;

/**
 * Shared validation regex/pattern constants for security-module DTOs.
 */
public final class SecurityValidationPatterns {

    private SecurityValidationPatterns() {
    }

    // Examples: 01XXXXXXXXX, +8801XXXXXXXXX, 008801XXXXXXXXX
    public static final String BANGLADESH_PHONE_REGEX = "^(?:(?:\\+|00)88|01)?\\d{11}$";

    // Examples: 9XXXXXXXXX, +919XXXXXXXXX, 00919XXXXXXXXX
    public static final String INDIA_PHONE_REGEX = "^(?:(?:\\+|00)91)?[6-9]\\d{9}$";

    public static final String BD_IN_CONTACT_REGEX = "(?:" + BANGLADESH_PHONE_REGEX + ")|(?:" + INDIA_PHONE_REGEX + ")";
}

