package com.central.security.core.urls;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.TYPE;

import com.central.security.core.urls.model.dto.UrlDTO;
import com.central.security.core.urls.service.UrlService;
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

import org.springframework.web.servlet.HandlerMapping;


/**
 * Class-level constraint that validates the composite (endpoint, method) pair is unique.
 * Matches the DB constraint {@code uk_endpoint_method} on the {@code Url} table.
 *
 * Placed on {@code UrlDTO} so both fields are accessible during validation.
 */
@Target({TYPE, ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = UrlsEndpointUnique.UrlsCompositeUniqueValidator.class)
public @interface UrlsEndpointUnique {

    String message() default "{exists.urls.endpoint.method}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class UrlsCompositeUniqueValidator implements ConstraintValidator<UrlsEndpointUnique, UrlDTO> {

        private final UrlService urlsService;
        private final HttpServletRequest request;

        public UrlsCompositeUniqueValidator(final UrlService urlsService,
                                            final HttpServletRequest request) {
            this.urlsService = urlsService;
            this.request = request;
        }

        @Override
        public boolean isValid(final UrlDTO dto, final ConstraintValidatorContext cvContext) {
            if (dto == null || dto.getEndpoint() == null || dto.getMethod() == null) {
                return true; // let @NotNull handle nulls
            }

            @SuppressWarnings("unchecked")
            final Map<String, String> pathVariables =
                    (Map<String, String>) request.getAttribute(
                            HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
            final String currentId = pathVariables != null ? pathVariables.get("id") : null;

            if (currentId != null) {
                // Update path: allow the DTO to keep its own (endpoint, method)
                final var existing = urlsService.get(Long.parseLong(currentId));
                if (existing != null
                        && dto.getEndpoint().equalsIgnoreCase(existing.data.getEndpoint())
                        && dto.getMethod().equalsIgnoreCase(existing.data.getMethod())) {
                    return true; // unchanged combination
                }
            }

            return !urlsService.endpointAndMethodExists(dto.getEndpoint(), dto.getMethod());
        }
    }
}
