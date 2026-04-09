package com.socialmanager.repository;

import com.socialmanager.model.Platform;
import com.socialmanager.model.SocialAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SocialAccountRepository extends JpaRepository<SocialAccount, UUID> {
    Optional<SocialAccount> findByUserIdAndPlatformAndExternalAccountId(
        UUID userId,
        Platform platform,
        String externalAccountId
    );
}