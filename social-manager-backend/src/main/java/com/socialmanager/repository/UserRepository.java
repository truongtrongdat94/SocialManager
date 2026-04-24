package com.socialmanager.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository; // Nhớ import cái này

import com.socialmanager.model.User;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    
    // Thêm đúng dòng này vào là xong:
    Optional<User> findByEmail(String email);
}