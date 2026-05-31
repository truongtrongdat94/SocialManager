package com.socialmanager.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.socialmanager.model.User;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    // Tìm user bằng Email (Cần cho cả login thường và login AI của Tiến)
    Optional<User> findByEmail(String email);

    // Tìm user bằng Username (Của nhánh Auth)
    Optional<User> findByUsername(String username);

    // Tìm user bằng Google ID (Để sau này tích hợp login Google/OAuth2)
    Optional<User> findByGoogleId(String googleId);
}