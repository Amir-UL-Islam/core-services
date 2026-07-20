package com.central.security.core.security.util;

import com.central.security.core.users.repository.UsersRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Utility for managing token versioning and invalidation.
 * When user permissions change (role/privilege updates), we increment tokenVersion
 * to invalidate all existing tokens for that user, forcing re-authentication.
 */
@Component
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class)
@Slf4j
public class TokenVersionManager {

    private final UsersRepository usersRepository;

    /**
     * Invalidate all access tokens for a user.
     * Typically called when:
     * - User's roles change
     * - User's direct privileges change
     * - User is disabled/locked
     *
     * @param userId The user whose tokens should be invalidated
     * @return true if successful, false if user not found
     */
    public boolean invalidateAccessTokens(Long userId) {
        return usersRepository.findById(userId)
                .map(user -> {
                    int newVersion = user.getTokenVersion() + 1;
                    user.setTokenVersion(newVersion);
                    usersRepository.save(user);
                    log.info("Invalidated access tokens for user {}: new version = {}", userId, newVersion);
                    return true;
                })
                .orElse(false);
    }

    /**
     * Invalidate all refresh tokens for a user.
     * Typically called when:
     * - User explicitly logs out
     * - User password changes
     * - Admin revokes refresh privileges
     *
     * @param userId The user whose refresh tokens should be invalidated
     * @return true if successful, false if user not found
     */
    public boolean invalidateRefreshTokens(Long userId) {
        return usersRepository.findById(userId)
                .map(user -> {
                    int newVersion = user.getRefreshTokenVersion() + 1;
                    user.setRefreshTokenVersion(newVersion);
                    usersRepository.save(user);
                    log.info("Invalidated refresh tokens for user {}: new version = {}", userId, newVersion);
                    return true;
                })
                .orElse(false);
    }

    /**
     * Invalidate both access and refresh tokens for a user.
     * Used for complete session termination.
     *
     * @param userId The user whose all tokens should be invalidated
     * @return true if successful
     */
    public boolean invalidateAllTokens(Long userId) {
        return invalidateAccessTokens(userId) && invalidateRefreshTokens(userId);
    }

    /**
     * Invalidate all tokens for all users (system-wide).
     * Used for major security events like privilege system changes.
     * WARNING: This will force all users to re-authenticate!
     *
     * @return Number of users affected
     */
    public long invalidateAllUserTokens() {
        long count = usersRepository.count();
        usersRepository.findAll().forEach(user -> {
            user.setTokenVersion(user.getTokenVersion() + 1);
            user.setRefreshTokenVersion(user.getRefreshTokenVersion() + 1);
        });
        usersRepository.flush();
        log.warn("Invalidated all tokens for {} users - system-wide token invalidation", count);
        return count;
    }
}

