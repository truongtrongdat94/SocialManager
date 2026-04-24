package com.socialmanager.repository;

import com.socialmanager.model.SocialAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SocialAccountRepository extends JpaRepository<SocialAccount, UUID> {

	List<SocialAccount> findByUser_IdOrderByCreatedAtDesc(UUID userId);

	Optional<SocialAccount> findByIdAndUser_Id(UUID id, UUID userId);
}
