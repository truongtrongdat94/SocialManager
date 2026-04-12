package com.socialmanager.repository;

import com.socialmanager.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    // Interface này sẽ giúp bạn tìm User bằng ID (UUID) một cách tự động
}