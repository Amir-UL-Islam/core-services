package com.central.security.core.users;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.central.security.core.security.filters.RequestBodyCachingFilter;
import com.central.security.core.users.model.entity.Users;
import com.central.security.core.users.service.UsersService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Map;

import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.WebUtils;
import org.springframework.web.servlet.HandlerMapping;


/**
 * Validate that the username value isn't taken yet.
 * When the username belongs to an unverified account, a distinct message is reported
 * via {@link #unverifiedMessage()} so the client knows to complete verification.
 */
@Target({FIELD, METHOD, ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(
        validatedBy = UsersUsernameUnique.UsersUsernameUniqueValidator.class
)
public @interface UsersUsernameUnique {

    String message() default "{exists.users.username}";

    /**
     * Message used when the username exists, but the account has not been verified yet.
     */
    String unverifiedMessage() default "{exists.users.username.unverified}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class UsersUsernameUniqueValidator implements ConstraintValidator<UsersUsernameUnique, String> {

        private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

        private final UsersService usersService;
        private final HttpServletRequest request;
        private String unverifiedMessage;

        public UsersUsernameUniqueValidator(final UsersService usersService,
                                            final HttpServletRequest request) {
            this.usersService = usersService;
            this.request = request;
        }

        @Override
        public void initialize(final UsersUsernameUnique annotation) {
            this.unverifiedMessage = annotation.unverifiedMessage();
        }

        @Override
        public boolean isValid(final String value, final ConstraintValidatorContext cvContext) {
            if (value == null || value.isBlank()) {
                // no value present
                return true;
            }
            final String normalizedValue = value.trim();
            final Long currentId = resolveCurrentUserId();
            if (currentId != null) {
                final var currentUser = usersService.findById(currentId);
                if (currentUser.isPresent()
                        && normalizedValue.equalsIgnoreCase(currentUser.get().getUsername())) {
                    // value hasn't changed
                    return true;
                }
            }

            final Users existing = usersService.findByUsername(normalizedValue);
            if (existing != null) {
                if (existing.getId().equals(currentId)) {
                    return true;
                }
//                if (Boolean.TRUE.equals(existing.getAccountEnabled())) {
//                    return false;
//                }
                cvContext.disableDefaultConstraintViolation();
                if (existing.getAccountEnabled() == null || !existing.getAccountEnabled()) {
                    cvContext.buildConstraintViolationWithTemplate(unverifiedMessage)
                            .addConstraintViolation();
                }
                if (existing.getAccountEnabled() != null && existing.getAccountEnabled()) {
                    cvContext.buildConstraintViolationWithTemplate(cvContext.getDefaultConstraintMessageTemplate())
                            .addConstraintViolation();
                }
                return false;
            }
            return true;
        }

        private Long resolveCurrentUserId() {
            if (request == null) {
                return null;
            }

            // Backward-compatible: support endpoints that still use /{id}
            @SuppressWarnings("unchecked") final Map<String, String> pathVariables =
                    (Map<String, String>) request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
            if (pathVariables != null) {
                final Long idFromPath = toLong(pathVariables.get("id"));
                if (idFromPath != null) {
                    return idFromPath;
                }
            }

            // Non-path update style: id may arrive as request parameter
            final Long idFromParam = toLong(request.getParameter("id"));
            if (idFromParam != null) {
                return idFromParam;
            }

            // Non-path update style: id may arrive inside JSON body as data.id or id
            ContentCachingRequestWrapper wrapper = (ContentCachingRequestWrapper)
                    request.getAttribute(RequestBodyCachingFilter.CACHED_REQUEST_ATTRIBUTE);
            if (wrapper == null) {
                wrapper = WebUtils.getNativeRequest(request, ContentCachingRequestWrapper.class);
            }

            if (wrapper != null) {
                final byte[] body = wrapper.getContentAsByteArray();
                if (body.length > 0) {
                    try {
                        final JsonNode root = OBJECT_MAPPER.readTree(body);
                        Long id = toLong(root.path("data").path("id").asText(null));
                        if (id != null) {
                            return id;
                        }
                        id = toLong(root.path("id").asText(null));
                        if (id != null) {
                            return id;
                        }
                    } catch (Exception ignored) {
                        // Ignore parse errors here and fall back to create-mode uniqueness check
                    }
                }
            }
            return null;
        }

        private Long toLong(final String value) {
            if (value == null || value.isBlank()) {
                return null;
            }
            try {
                return Long.parseLong(value.trim());
            } catch (NumberFormatException ex) {
                return null;
            }
        }
    }

}
