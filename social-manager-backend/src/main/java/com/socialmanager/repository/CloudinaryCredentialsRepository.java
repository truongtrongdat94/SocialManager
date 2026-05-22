package com.socialmanager.repository;

import com.socialmanager.model.CloudinaryCredentials;
import com.socialmanager.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CloudinaryCredentialsRepository extends JpaRepository<CloudinaryCredentials, UUID> {
    Optional<CloudinaryCredentials> findByUser(User user);
}
