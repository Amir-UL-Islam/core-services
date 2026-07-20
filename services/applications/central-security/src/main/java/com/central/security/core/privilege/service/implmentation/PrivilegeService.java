package com.central.security.core.privilege.service.implmentation;

import com.central.security.util.CustomCollectors;
import com.central.security.util.NotFoundException;
import com.problemfighter.pfspring.restapi.rr.response.PageableResponse;
import com.central.security.core.privilege.model.dto.PrivilegeDTO;
import com.central.security.core.privilege.model.entity.Privilege;
import com.central.security.core.privilege.repository.PrivilegeRepository;
import com.central.security.core.urls.model.EndpointPatternMatcher;
import com.central.security.core.urls.model.entity.Url;
import com.problemfighter.pfspring.restapi.rr.RequestResponse;
import com.problemfighter.pfspring.restapi.rr.response.DetailsResponse;

import java.util.*;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@Transactional(rollbackFor = Exception.class)
@RequiredArgsConstructor
@Slf4j
public class PrivilegeService implements RequestResponse {

    private final PrivilegeRepository privilegeRepository;
    private final PrivilegeServiceCacheable cacheableService;


    public PageableResponse<PrivilegeDTO> findAll(Pageable pageable) {
        return responseProcessor().response(privilegeRepository.findAll(pageable), PrivilegeDTO.class);
    }

    public DetailsResponse<PrivilegeDTO> getDetails(final Long id) {
        return responseProcessor().response(privilegeRepository.findById(id), PrivilegeDTO.class);
    }

    /**
     * Backward-compatible DTO accessor used by validation annotations.
     */
    public PrivilegeDTO get(final Long id) {
        return privilegeRepository.findById(id)
                .map(privilege -> responseProcessor().entityToDTO(privilege, PrivilegeDTO.class))
                .orElseThrow(NotFoundException::new);
    }


    public boolean nameExists(final String name) {
        return privilegeRepository.existsByNameIgnoreCase(name);
    }

    public Map<Long, String> getPrivilegeValues() {
        return privilegeRepository.findAll(Sort.by("id"))
                .stream()
                .collect(CustomCollectors.toSortedMap(Privilege::getId, Privilege::getName));
    }


    /**
     * Check if the user has permission to access the requested endpoint.
     * <p>
     * Process:
     * 1. Extract request path and HTTP method
     * 2. Find all endpoint patterns from DB that could match this request
     * 3. For each pattern, use EndpointPatternMatcher to check if the request path matches
     * 4. If any pattern matches, check if the user has the required privilege
     * 5. Policy-first: missing policies deny access
     *
     * @param authentication Current Spring Security authentication
     * @param request        The HTTP request
     * @return true if the user is authorized, false otherwise
     */
    public boolean hasPermission(Authentication authentication, HttpServletRequest request) {
        String requestPath = request.getRequestURI();
        String httpMethod = request.getMethod();

        // Policy-first applies to API routes. Static assets and non-API routes are handled elsewhere.
        if (!requestPath.startsWith("/api")) {
            return true;
        }

        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        // Get user authorities at once
        Collection<? extends GrantedAuthority> userAuthorities = authentication.getAuthorities();
        if (userAuthorities.isEmpty()) {
            return false;
        }

        // Find all endpoint definitions (patterns) for this HTTP method
        // Note: We query broadly by method, then do client-side pattern matching
        // to avoid complex regex queries in database
        List<Privilege> matchingPrivileges = new ArrayList<>();

        // Load all privileges with their URLs eagerly (result is Caffeine-cached to avoid N+1 on every request)
        List<Privilege> allPrivileges = cacheableService.loadAllPrivilegesWithUrls();

        for (Privilege privilege : allPrivileges) {
            for (Url url : privilege.getUrls()) {
                // Check HTTP method
                if (!url.getMethod().equalsIgnoreCase(httpMethod)) {
                    continue;
                }

                // Check if request path matches this endpoint pattern
                try {
                    EndpointPatternMatcher matcher = new EndpointPatternMatcher(url.getEndpoint());
                    if (matcher.matches(requestPath)) {
                        matchingPrivileges.add(privilege);
                        break; // Found a match for this privilege, move to next privilege
                    }
                } catch (Exception e) {
                    log.warn("Invalid endpoint pattern: {}", url.getEndpoint(), e);
                }
            }
        }

        // Policy-first mode: explicit URL policy is required for every API endpoint.
        if (matchingPrivileges.isEmpty()) {
            log.debug("No policy defined for {} {}", httpMethod, requestPath);
            return false;
        }

        // Check if user has any of the required privileges
        return matchingPrivileges.stream()
                .anyMatch(privilege ->
                        userAuthorities.stream()
                                .anyMatch(authority ->
                                        Objects.equals(authority.getAuthority(), privilege.getName())
                                )
                );
    }

}
