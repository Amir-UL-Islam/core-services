package com.central.security.core.privilege.service.implmentation;

import com.central.security.config.CacheConfig;
import com.central.security.events.BeforeDeletePrivilege;
import com.central.security.util.NotFoundException;
import com.problemfighter.pfspring.restapi.rr.RequestResponse;
import com.problemfighter.pfspring.restapi.rr.request.RequestData;
import com.problemfighter.pfspring.restapi.rr.response.MessageResponse;
import com.central.security.core.privilege.model.dto.PrivilegeDTO;
import com.central.security.core.privilege.model.entity.Privilege;
import com.central.security.core.privilege.repository.PrivilegeRepository;
import com.central.security.core.security.audit.model.entity.PolicyChangeAudit;
import com.central.security.core.security.audit.service.PolicyChangeAuditService;
import com.central.security.core.security.util.TokenVersionManager;
import com.central.security.core.urls.model.entity.Url;
import com.central.security.core.urls.repository.UrlsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PrivilegeServiceCacheable implements RequestResponse {
    private final UrlsRepository urlsRepository;
    private final ApplicationEventPublisher publisher;
    private final PrivilegeRepository privilegeRepository;
    private final TokenVersionManager tokenVersionManager;
    private final PolicyChangeAuditService policyChangeAuditService;

    @CacheEvict(value = CacheConfig.CACHE_PRIVILEGES, allEntries = true)
    public MessageResponse create(final RequestData<PrivilegeDTO> data) {
        final Privilege privilege = new Privilege();
        requestProcessor().process(data, privilege);
        final Privilege saved = privilegeRepository.save(privilege);
        policyChangeAuditService.recordChange(
                PolicyChangeAudit.PolicyType.PRIVILEGE, saved.getId(),
                PolicyChangeAudit.ChangeType.CREATE,
                currentUserId(), currentUsername(),
                null, saved.getName(),
                "Privilege created");
        return responseProcessor().response("Privilege created with ID: " + saved.getId());
    }

    @CacheEvict(value = CacheConfig.CACHE_PRIVILEGES, allEntries = true)
    public MessageResponse update(final RequestData<PrivilegeDTO> data) {
        requestProcessor().validateId(data.getData().getId(), "Privilege ID can't be null");
        final Long id = data.getData().getId();
        final Privilege privilege = privilegeRepository.findById(id)
                .orElseThrow(NotFoundException::new);
        final String previousName = privilege.getName();
        requestProcessor().process(data, privilege);
        privilegeRepository.save(privilege);

        policyChangeAuditService.recordChange(
                PolicyChangeAudit.PolicyType.PRIVILEGE, id,
                PolicyChangeAudit.ChangeType.UPDATE,
                currentUserId(), currentUsername(),
                previousName, privilege.getName(),
                "Privilege updated");

        // Invalidate tokens for all users who have this privilege via any role
        tokenVersionManager.invalidateAllUserTokens();
        log.info("Invalidated all user tokens after privilege {} update", id);
        return responseProcessor().response("Privilege updated with ID: " + id);
    }

    @CacheEvict(value = CacheConfig.CACHE_PRIVILEGES, allEntries = true)
    public MessageResponse delete(final Long id) {
        final Privilege privilege = privilegeRepository.findById(id)
                .orElseThrow(NotFoundException::new);
        final String name = privilege.getName();

        policyChangeAuditService.recordChange(
                PolicyChangeAudit.PolicyType.PRIVILEGE, id,
                PolicyChangeAudit.ChangeType.DELETE,
                currentUserId(), currentUsername(),
                name, null,
                "Privilege deleted");

        publisher.publishEvent(new BeforeDeletePrivilege(id));
        privilegeRepository.delete(privilege);

        // Invalidate all tokens since privilege removal affects all roles that had it
        tokenVersionManager.invalidateAllUserTokens();
        log.info("Invalidated all user tokens after privilege {} deletion", id);
        return responseProcessor().response("Privilege deleted with ID: " + id);
    }

    @CacheEvict(value = CacheConfig.CACHE_PRIVILEGES, allEntries = true)
    public MessageResponse assignUrl(Long privilegeId, Long urlId) {
        Privilege privilege = privilegeRepository.findById(privilegeId).orElseThrow();
        Url url = urlsRepository.findById(urlId).orElseThrow();

        privilege.getUrls().add(url);
        urlsRepository.save(url);
        privilegeRepository.save(privilege);

        policyChangeAuditService.recordChange(
                PolicyChangeAudit.PolicyType.PRIVILEGE, privilegeId,
                PolicyChangeAudit.ChangeType.ASSIGN,
                currentUserId(), currentUsername(),
                null, "url:" + urlId,
                "URL assigned to privilege");
        return responseProcessor().response("URL " + urlId + " assigned to privilege " + privilegeId);
    }

    @CacheEvict(value = CacheConfig.CACHE_PRIVILEGES, allEntries = true)
    public MessageResponse removeAssignUrl(Long privilegeId, Long urlId) {
        Privilege privilege = privilegeRepository.findById(privilegeId).orElseThrow();
        Url url = urlsRepository.findById(urlId).orElseThrow();

        privilege.getUrls().remove(url);
        urlsRepository.save(url);
        privilegeRepository.save(privilege);

        policyChangeAuditService.recordChange(
                PolicyChangeAudit.PolicyType.PRIVILEGE, privilegeId,
                PolicyChangeAudit.ChangeType.UNASSIGN,
                currentUserId(), currentUsername(),
                "url:" + urlId, null,
                "URL removed from privilege");
        return responseProcessor().response("URL " + urlId + " removed from privilege " + privilegeId);
    }

    /**
     * Load all privileges with their URL mappings eagerly.
     * Result is cached by Caffeine (TTL 5 min) and evicted on any privilege/URL writing.
     * This eliminates the N+1 query pattern that previously hit the DB on every ACL check.
     */
    @Cacheable(value = CacheConfig.CACHE_PRIVILEGES, key = "'all'")
    @Transactional(readOnly = true)
    public List<Privilege> loadAllPrivilegesWithUrls() {
        return privilegeRepository.findAllWithUrls();
    }

    // -------------------------------------------------------------------------
    // Audit helpers
    // -------------------------------------------------------------------------

    private Long currentUserId() {
        try {
            final Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof com.central.security.core.users.model.entity.Users u) {
                return u.getId();
            }
        } catch (Exception ignored) { /* best-effort */ }
        return null;
    }

    private String currentUsername() {
        try {
            final Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof UserDetails ud) {
                return ud.getUsername();
            }
        } catch (Exception ignored) { /* best-effort */ }
        return "system";
    }
}
