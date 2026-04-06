package com.socialmanager.service;

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
    public String login(LoginRequest request) {
        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BadCredentialsException("Invalid credentials");
        }
        return jwtUtil.generateToken(user.getUsername());
    }

    // Task 4.12: getCurrentUser — try findByEmail, then findByUsername
    public UserDto getCurrentUser(String subject) {
        User user = userRepository.findByEmail(subject)
                .or(() -> userRepository.findByUsername(subject))
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return new UserDto(user.getId(), user.getEmail(), user.getUsername(), user.getName());
    }
}
