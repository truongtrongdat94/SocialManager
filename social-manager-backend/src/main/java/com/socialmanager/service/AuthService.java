package com.socialmanager.service;

import com.socialmanager.dto.AuthResponse;
import com.socialmanager.dto.LoginRequest;
import com.socialmanager.dto.RegisterRequest;
import com.socialmanager.dto.UserDto;
import com.socialmanager.exception.ResourceNotFoundException;
import com.socialmanager.exception.UsernameAlreadyTakenException;
import com.socialmanager.model.User;
import com.socialmanager.repository.UserRepository;
import com.socialmanager.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    // Task 4.1: processOAuthUser — find by email, create or update googleId
    public User processOAuthUser(String email, String name, String googleId) {
        Optional<User> existing = userRepository.findByEmail(email);
        if (existing.isPresent()) {
            User user = existing.get();
            if (user.getGoogleId() == null) {
                user.setGoogleId(googleId);
                userRepository.save(user);
            }
            return user;
        }
        User newUser = User.builder()
                .email(email)
                .name(name)
                .googleId(googleId)
                .build();
        return userRepository.save(newUser);
    }

    // Task 4.4: register — check unique username, BCrypt hash, persist
    public UserDto register(RegisterRequest request) {
        if (userRepository.findByUsername(request.username()).isPresent()) {
            throw new UsernameAlreadyTakenException("Username already taken");
        }
        User user = User.builder()
                .username(request.username())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .name(request.name())
                .build();
        User saved = userRepository.save(user);
        return new UserDto(saved.getId(), saved.getEmail(), saved.getUsername(), saved.getName());
    }

    // Task 4.7: login — find by username, verify BCrypt, return JWT
    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BadCredentialsException("Invalid credentials");
        }
        
        String accessToken = jwtUtil.generateToken(user.getUsername());
        String refreshToken = jwtUtil.generateRefreshToken(user.getUsername());
        
        // Save refresh token to database
        user.setRefreshToken(refreshToken);
        user.setRefreshTokenExpiresAt(
            LocalDateTime.now().plusSeconds(jwtUtil.getRefreshExpirationMs() / 1000)
        );
        userRepository.save(user);
        
        return new AuthResponse(accessToken, refreshToken);
    }

    // Refresh access token using refresh token
    @Transactional
    public AuthResponse refreshAccessToken(String refreshToken) {
        // Validate refresh token format
        if (!jwtUtil.validateToken(refreshToken)) {
            throw new BadCredentialsException("Invalid refresh token");
        }
        
        // Get username from token
        String username = jwtUtil.getUsernameFromToken(refreshToken);
        
        // Find user and verify refresh token
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BadCredentialsException("User not found"));
        
        if (user.getRefreshToken() == null || !user.getRefreshToken().equals(refreshToken)) {
            throw new BadCredentialsException("Invalid refresh token");
        }
        
        if (user.getRefreshTokenExpiresAt() == null || 
            user.getRefreshTokenExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadCredentialsException("Refresh token expired");
        }
        
        // Generate new tokens
        String newAccessToken = jwtUtil.generateToken(username);
        String newRefreshToken = jwtUtil.generateRefreshToken(username);
        
        // Update refresh token in database
        user.setRefreshToken(newRefreshToken);
        user.setRefreshTokenExpiresAt(
            LocalDateTime.now().plusSeconds(jwtUtil.getRefreshExpirationMs() / 1000)
        );
        userRepository.save(user);
        
        return new AuthResponse(newAccessToken, newRefreshToken);
    }

    // Task 4.12: getCurrentUser — try findByEmail, then findByUsername
    public UserDto getCurrentUser(String subject) {
        User user = userRepository.findByEmail(subject)
                .or(() -> userRepository.findByUsername(subject))
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return new UserDto(user.getId(), user.getEmail(), user.getUsername(), user.getName());
    }

    // Save user (used by OAuth2)
    public User saveUser(User user) {
        return userRepository.save(user);
    }
}
